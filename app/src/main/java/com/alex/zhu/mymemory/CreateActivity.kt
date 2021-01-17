package com.alex.zhu.mymemory

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alex.zhu.mymemory.models.BoardSize
import com.alex.zhu.mymemory.utils.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {

    companion object {
        // Doesn't really matter what the request codes are but as a best practice we keep them
        // distinct
        private const val PICK_PHOTO_CODE = 165
        private const val READ_EXTERNAL_PHOTOS_CODE = 322

        private const val READ_PHOTOS_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE

        private const val TAG = "CreateActivity"

        private const val MIN_GAME_NAME_LENGTH = 3
        private const val MAX_GAME_NAME_LENGTH = 14
    }
    // Get a reference to the three views on the screen: the RecyclerView, the EditText and
    // the button, all of these will be lateinit var because we are going to declare them
    // as member variables but actually set the value in OnCreate
    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button
    private lateinit var pbUploading: ProgressBar

    // We want boardSize to be a member variable so we can reference it across multiple methods
    private lateinit var boardSize: BoardSize
    private var numImagesRequired = -1

    // This is the variable for the list of images the user has chosen
    // A URI is a Uniform Resource Identifier that we can think of as like a string, which
    // unambiguously identifies "where does a particular resource live?"
    // The resource in our case is an image which lives on the phone
    // The URI is describing "What is the directory path to locate this photo?"
    // When the user has picked two or three photos for example, there will be two to three
    // elements (URI's) in this list
    private val chosenImageUris = mutableListOf<Uri>()

    private lateinit var adapter: ImagePickerAdapter

    private val storage = Firebase.storage
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        rvImagePicker = findViewById (R.id.rvImagePicker)
        etGameName = findViewById (R.id.etGameName)
        btnSave = findViewById (R.id.btnSave)
        pbUploading = findViewById (R.id.pbUploading)

        // Pull the data out from the intent
        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize

        // Based on the boardSize we want to modify the title of the CreateActivity to indicate
        // how many pictures the user has to select from their phone in order to make a valid game

        // Add a back button to the default appearance which allows the user to easily exit out
        // flow and go back to the main activity if they want
        // This will modify the action bar to show a back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Add a click listener on the save button
        btnSave.setOnClickListener {
            saveDataToFirebase()
        }

        // Set a maximum length of the game name
        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))

        etGameName.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Every time the user makes any modification in that edit text, we will
                // potentially enable to save button
                btnSave.isEnabled = shouldEnableSaveButton()
            }

        })

        numImagesRequired = boardSize.getNumPairs()
        // Error because supportActionBar technically is nullable and so we can use a question
        // mark operator which says only call this attribute if supportActionBar is not null
        supportActionBar?.title = "Choose images (0/ $numImagesRequired)"

        // Similar to the MainActivity, the RecyclerView will have two core components: the adapter
        // and the LayoutManager
        // Set the adapter
        // Second parameter is a list of the images the user has chosen (create a new variable for)
        // We need to pass in this fourth parameter which is the instance of the interface
        // (from ImagePickerAdapter)
        adapter = ImagePickerAdapter(this, chosenImageUris, boardSize,
            object: ImagePickerAdapter.ImageClickListener {

            override fun onPlaceHolderClicked() {
                // Now, onPlaceHolderClicked means the user has tapped on one of the gray squares
                // (image views)
                // So here is where we are going to launch the photo choosing flow

                // First parameter is the activity
                // Second parameter is a string representing the permission
                // (we will define a constant)
                if (isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)) {
                    launchIntentForPhotos()
                } else {
                    // Third Parameter is the request code that we defined
                        // This requestPermission function is going to launch an Android system
                            // dialog asking the user if they want to allow this app the read
                                // external storage permission
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTOS_CODE)
                }

                // Notice this the second time we are dealing with intents now, we had one intent
                // to go from the MainActivity to the CreateActivity we're also going to have an
                // intent here to launch the flow for the user to pick a photo
                // This is called an Implicit Intent because we don't actually care which application
                // on the phone will handle this intent, this request, but we just want the user to
                // choose something that will allow them to pick an image for our application to consume
            }

        })
        rvImagePicker.adapter = adapter

                // Here we are guarenteeing that that the RecyclerView dimensions won't change because
        // we have allocated just enough space for it
        rvImagePicker.setHasFixedSize(true)
        // Second parameter is the number of columns
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())

    }

    // Regardless of whether the user accepts or rejects we will get a call back called
    // onRequestPermissionsResult and that is what we will override
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // Checks if the requestCode is equal to the READ_EXTERNAL_PHOTOS_CODE that we used to
        // launch the dialog
        if (requestCode == READ_EXTERNAL_PHOTOS_CODE) {
            // grantResults should be non-empty because we are getting the results here
                // We are checking if that is equal to permission granted
                    // (in other words the user granted permission)
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchIntentForPhotos()
            } else {
                Toast.makeText(this, "In order to create a custom game, you need to provide access" +
                        "to your photos", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    // We need to actually take action when the user has tapped on the back button
    // This is similar to what we have done before
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // this id: android.R.id.home is defined within the android system within the android SDK,
        // it is not a menu item that we added so we need the android prefix here
        if (item.itemId == android.R.id.home) {
            // Then we want to finish this activity and go back to the main activity
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // If requestCode and PICK_PHOTO_CODE match then then we know that we are processing the
        // onActivityResult for the right intent
        // Check if the data we get back is invalid
        if (requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null) {
            // Log at the warning level (Log.w)
            Log.w(TAG, "Did not get data back form the launched activity, user likely " +
                    "canceled the selection flow")
            // Return early, which means the rest of the code terminates when conditions are not met
            return
        }
        // There are two different attributes that we care about on the data intent
        // First, if the application that's launched in order for the user to pick a photo only
        // supports picking one photo then the selected photo will come back in the data.data
        // attribute, which is a URI
        val selectedUri = data.data

        // On the other hand, if the application launched supports picking multiple photos and the
        // user picks multiple photos then that data will come back  as part of data.clipData
        val clipData = data.clipData

        if (clipData != null) {
            // In this case we will log (at info level), the clipData, numImages, how many items
                // are in the clipData, along with the the actual contents of the clipData object
            Log.i(TAG, "clipData numImages ${clipData.itemCount}: $clipData")
            // Iterate through the clipData:
            for (i in 0 until clipData.itemCount) {
                // Retrieve the clipItem at that position in the clipData
                val clipItem = clipData.getItemAt(i)

                // Check if we still need to populate the image URI's
                if (chosenImageUris.size < numImagesRequired) {
                    chosenImageUris.add(clipItem.uri)
                }
            }
            // Here we can unconditionally add the selectedUri to our chosenUri list since we know
            // the user has selected a gray square whereas above, we need to check if we still have
            // sufficient space for more image URI's
        } else if (selectedUri != null) {
            Log.i(TAG, "data: $selectedUri")
            chosenImageUris.add(selectedUri)
        }
        // Now that we have added the image URI's into the list, we can notify the adapter that the
        // underlying data set has changed
        // In order to do that we need to make the adapter a property or member variable of the class
        adapter.notifyDataSetChanged()

        // Update the title of the activity to inform the user of how many images they have
        // picked so far
        supportActionBar?.title = "Choose images (${chosenImageUris.size} / $numImagesRequired)"

        btnSave.isEnabled = shouldEnableSaveButton()
    }

    // This function is responsible for deciding whether we should enable the save button or not
    private fun shouldEnableSaveButton(): Boolean {
        if (chosenImageUris.size != numImagesRequired) {
            return false
        }
        if (etGameName.text.isBlank() || etGameName.text.length < MIN_GAME_NAME_LENGTH) {
            return false
        }
        return true
    }

    private fun launchIntentForPhotos() {
        // Because this is an implicit intent we are going to just pass in a intent action here
        val intent = Intent (Intent.ACTION_PICK)
        // We only want images, we don't want video files, pdf or any other file
        intent.type = "image/*"
        // We will pass in one extra into the intent
        // EXTRA_ALLOW_MULTIPLE is a constant on the intent class\
        // Means that, if the app that the user opens up supports it, we want the user to be able
        // to select multiple images
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)

        // The second parameter to startActivityForResult, similar to last time, is a request code
        // that can name and define inside the companion object of CreateActivity
        // (that we will now create)
        startActivityForResult(Intent.createChooser(intent, "Choose images"), PICK_PHOTO_CODE)
    }

    // To lower the photo quality to ensure a function game even if there are a lot of user
    private fun saveDataToFirebase() {
        Log.i(TAG, "saveDataToFirebase")
        // Disable the save button, because the user would be attempting to create the same game again
        btnSave.isEnabled = false

        val customGameName = etGameName.text.toString()
        // Check on Firestore to make sure we are not overwriting someone else's data
        // Target the same collection "games" and check if we already have a document with this name
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            if (document != null && document.data != null) {
                AlertDialog.Builder(this)
                    .setTitle("Name taken")
                    .setMessage("A game already exists with the name '$customGameName'. Please choose another name")
                    .setPositiveButton("OK", null)
                    .show()
                // if there was a failure for whatever reason, user can try to save again
                btnSave.isEnabled = true
            } else {
                handleImageUploading(customGameName)
            }
            // add failure listener just in case for whatever reason we are unable to retrieve this
            // document, so we can debug
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Encountered error while saving memory game", exception)
            Toast.makeText( this, "Encountered error while saving memory game", Toast.LENGTH_SHORT).show()
            // if there was a failure for whatever reason, user can try to save again
            btnSave.isEnabled = true
        }
    }

    private fun handleImageUploading(gameName: String) {
        // As soon as we start uploading images we want to set the progress bar to be visible
        pbUploading.visibility = View.VISIBLE

        var didEncounterError = false

        // We want to make note of the fact that this image has been successfully uploaded
        // To do that, we will keep an array of all the images that have been uploaded
        // so far
        val uploadedImageUrls = mutableListOf<String>()

        for ((index, photoUri) in chosenImageUris.withIndex()) {
            val imageByteArray = getImageByteArray(photoUri)
            // Our goal is to upload these files into Firebase Storage
            // The way that works is we're going to define a file path on where this image should
            // live in Firebase Storage
            // This file path should be dependant on the game name so if we ever wanted to look at
            // this from the Firebase console it will be easy to tell which images belong together

            // Inside this directory we can specify the current time in milliseconds
            val filePath = "images/$gameName/${System.currentTimeMillis()}-${index}.jpg"
            // The idea here is that if we have four images in the my_toronto game then that will
            // be saved in the images/my_toronto/(current time in milliseconds) 'dash'(-) 0,1,2,3
            // because there are 4 images, this is a nice way of combining all the images
            // associated with one memory game

            // With the previous two line, we have exactly what we need to upload the image to
            // storage, we have the actual underlying data for this image (imageByteArray) and
            // we have the file path

            // Get a reference to the location of where we want to save this photo
            // This is the operation that will actually do the expensive work of uploading the
            // bytes representing the image to Firebase Storage
            val photoReference = storage.reference.child(filePath)

            // A task that we have to wait for until it succeeds or fails
            photoReference.putBytes(imageByteArray)
                // Once the task concludes, execute this code and perform another task
                .continueWithTask {photoUploadTask ->
                    Log.i(TAG, "Uploaded bytes: ${photoUploadTask.result?.bytesTransferred}")
                    // End this lambda block with another task:
                    // Once this photo has been uploaded, we want the corresponding download url
                    photoReference.downloadUrl
                    // Getting this download url gives us a task, we need to wait for the completion
                    // of this task, that's an asynchronous operation

                    // In order to get notified of that we'll call:
                    // This addOnCompleteListener will be called for every image uploaded and we
                    // don't/can't control the order in which these are called
                }.addOnCompleteListener{downloadUrlTask ->
                    if (!downloadUrlTask.isSuccessful) {
                        // Log an error
                        Log.e(TAG, "Exception with the Firebase storage", downloadUrlTask.exception)
                        // Show a toast telling the user that the image upload failed
                        Toast.makeText(this,"Failed to upload image",Toast.LENGTH_SHORT).show()
                        didEncounterError = true
                        // If this happens we are going to do a premature return because there
                        // is no point continuing
                        return@addOnCompleteListener
                    }
                    if (didEncounterError == true) {
                        pbUploading.visibility = View.GONE
                        return@addOnCompleteListener
                    }
                    // Otherwise, we have been able to get a download url
                    // Look inside the task, look inside the string, cast is to a string and that
                    // will give us a url
                    val downloadUrl = downloadUrlTask.result.toString()
                    uploadedImageUrls.add(downloadUrl)
                    // Should be between 0 and 100
                    pbUploading.progress = uploadedImageUrls.size * 100 / chosenImageUris.size
                    Log.i(TAG, "Finished uploading $photoUri, num uploaded :${uploadedImageUrls.size}")
                    if (uploadedImageUrls.size == chosenImageUris.size) {
                        // The game name and uploaded image URLs are exactly the pieces of info
                        // we need to upload into Firestore
                        handleAllImagesUploaded(gameName, uploadedImageUrls)
                    }
                }


        }
    }

    private fun handleAllImagesUploaded(gameName: String, imageUrls: MutableList<String>) {
        // TODO: upload this info to Firestore
        // Reference the db object we have defined above and we are going to put this data into
        //   a document
        // Firestore organizes data into collections and documents, all documents have to live in
        //   a collection and one document represents one entity in our database
        // We call the collection "games"
        // Inside of games will be a list of all the custom games that people around the world
        //   have created
        // gameName represents the path of the document which is basically the name of this document

        db.collection("games").document(gameName)
            // Set the data associated with this game
            // All we want to do is associate all of the image urls to a key called images
            .set(mapOf("images" to imageUrls))

            // Gets notified when the previous above operation succeeded
                // the parameter for this lambda function will be a GameCreationTask which indicated
                // if this succeeded or failed
            .addOnCompleteListener { gameCreationTask ->
                pbUploading.visibility = View.GONE
                if (!gameCreationTask.isSuccessful) {
                    Log.e(TAG, "Exception with game creation", gameCreationTask.exception)
                    Toast.makeText(this, "Failed game creation", Toast.LENGTH_SHORT).show()
                    // Failed and should return early
                    return@addOnCompleteListener
                }
                Log.i(TAG, " Successfully created game '$gameName'")
                AlertDialog.Builder(this)
                    .setTitle("Upload complete! Lets's play your custom game '$gameName'")
                // user will only have one option on this alert dialog, which is to tap ok
                    .setPositiveButton("OK") { _,_ ->
                        // when they tap the OK button, we want to pass back to the main activity
                        // the game name which has been created
                        // Create an empty intent
                        val resultData = Intent()

                        // Inside of this intent we are going to pass the game name
                        // Had to create constant EXTRA_GAME_NAME
                        resultData.putExtra(EXTRA_GAME_NAME, gameName)
                        setResult(Activity.RESULT_OK, resultData)

                        //Finally, after we have set the data properly, we call finish
                        finish()
                    }.show() // Show this alert dialog
            }
    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        // Get the original bitmap based on the photo URI, that will depend on the API version that
        // this app is running on
        // What this conditional means is that if the phone operating system that we're running
        // on is running Android pie or higher then the original bitmap will come from running the
        // first two lines of code
        // Otherwise, on an older version, we will run the last line of code in order to get the
        // original bitmap
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        // We can compare the size now to after we scale it down
        Log.i(TAG, "Original width ${originalBitmap.width} and height ${originalBitmap.height}")
        // We can invent a method here called BitmapScaler.scaleToFitHeight
        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap, 250)
        Log.i(TAG, "Scaled width ${scaledBitmap.width} and height ${scaledBitmap.height}")

        // The last thing we need to do in this function is to return the ByteArray
        val byteArrayOutputStream = ByteArrayOutputStream()
        // 100 means no downgrade in quality and 0 means severe downgrade in quality
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

}