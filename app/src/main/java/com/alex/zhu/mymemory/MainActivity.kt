package com.alex.zhu.mymemory

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alex.zhu.mymemory.models.BoardSize
import com.alex.zhu.mymemory.models.MemoryGame
import com.alex.zhu.mymemory.models.UserImageList
import com.alex.zhu.mymemory.utils.EXTRA_BOARD_SIZE
import com.alex.zhu.mymemory.utils.EXTRA_GAME_NAME
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.dialog_download_board.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 248
    }



    // the reason that the following are lateinit var is because we know that these variables
    // will be set, but they will be set in the onCreate method which is invoked by the android
    // system, they are not going to be created at the time of construction of the MainActivity,
    // which is why this is a lateinit (a late initialization)
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView

    private lateinit var memoryGame: MemoryGame
    //lateinit because we know that memoryGame is going to be initialized properly but it will
    //only happen on create

    // We need areference to Firestore to replace default icons with custom images
    private val db = Firebase.firestore

    private var gameName: String? = null
    // null because when you're playing a default game with just the icons predefined, there
    // is no game name
    private var customGameImages: List<String>? = null

    private lateinit var adapter: MemoryBoardAdapter

    private lateinit var clRoot: CoordinatorLayout

    //a variable to replace the hardcoded value of numPieces
    private var boardSize: BoardSize = BoardSize.EASY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // now we can set the newly defined lateinit variables equal to the corresponding view in
        // the layout and we will do that by calling a special method findViewById and provide the
        // id that we assigned
        // For the recyclerView we give it an id of rvBoard
        rvBoard = findViewById(R.id.rvBoard)
        // Similarly,
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)

        // Improves the efficiency of getting to the MainActivity, hack for developer efficiency
        // Directly create the intent to navigate to the create activity
        // Explicit Intent, opposite is Implicit Intent
        //   val intent = Intent (this, CreateActivity::class.java)
        //   intent.putExtra(EXTRA_BOARD_SIZE, BoardSize.EASY)
        //   startActivity(intent) //not something we want to ship to production but worth investing
        // in to improve the efficiency with which we develop

        setupBoard()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // Based on the item id of this menu item we'll take a different action.
                // The only one we have right now is R.id.mi_refresh
            R.id.mi_refresh -> {
                //setup the game again
                if (memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()) {
                    showAlertDialog( "Quit your current game?", null, View.OnClickListener {
                        setupBoard()
                    })
                } else {
                    setupBoard()
                }
            }
            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom -> {
                showCreationDialog()
                return true
            }
            // Register a listener for when the menu item gets tapped
            R.id.mi_download -> {
                showDownloadDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if ( requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Retrieving the name of the custom game name
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if (customGameName == null) {
                Log.e(TAG, "Got null custom game name from CreateActivity")
                return
            }
            downloadGame(customGameName)
        }
            super.onActivityResult(requestCode, resultCode, data)

    }

    private fun showDownloadDialog() {
        // LayoutInflater to inflate the view which has the edit text
        val boardDownloadView = LayoutInflater.from(this).inflate(R.layout.dialog_download_board, null)
        showAlertDialog("Fetch memory game", boardDownloadView, View.OnClickListener {
            // Grab the text of the game name that the user wants to download
            val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString().trim()
            downloadGame(gameToDownload)
        })
    }

    private fun downloadGame(customGameName: String) {
        // The goal of this function is to query Firestore, retrieve the corresponding set of
        // image urls and use that to play the game of memory instead of our default icons
        // Query Firestore inside of the games collection
        db.collection("games").document(customGameName).get().addOnSuccessListener {document ->
            // We are going to get back a document which has one field called images and that
            // will correspond to a list of image urls (list of strings)
            val userImageList = document.toObject(UserImageList::class.java)
            if (userImageList?.images == null) {
                Log.e(TAG, "Invalid custom game data from Firestore")
                Snackbar.make(clRoot, "Sorry, we couldn't find any such game, '$customGameName", Snackbar.LENGTH_LONG).show()
                return@addOnSuccessListener
            }
            // with total num images, figure out board size
            val numCards = userImageList.images.size * 2
            boardSize = BoardSize.getByValue(numCards)
            customGameImages = userImageList.images

            // An optimization to get rid of the delay when displaying an image for the first time
            // Even though we're not displayed this into an imageview, just go ahead and download
            // it and fetch it so it's saved in the Picasso cache
            for (imageUrl in userImageList.images) {
                Picasso.get().load(imageUrl).fetch()
            }
            Snackbar.make(clRoot, "You're now playing '$customGameName'", Snackbar.LENGTH_LONG).show()

            // Set the property gamName equal to the lacoal variable game name
            gameName = customGameName
            setupBoard()

        }.addOnFailureListener{exception ->
            Log.e(TAG, "Exception when retrieving game", exception)
        }

    }


    // This showCreationDialog will be quite similar to showNewSizeDialog because the first thing
    // we want before we navigate the user to the creation flow is we need to understand what
    // size of a memory game they want to create.
    // We are going to reuse the exact same dialog_board_size and inflate that. That will be
    // we show on the dialog before we allow the user to go into the creation flow
    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        // Set a new value for the board size
        showAlertDialog("Create your own memory board", boardSizeView, View.OnClickListener {
            val desiredBoardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            // Navigate to a new activity
            // The way we navigate between activities or screens in Android is through something
            // called the intent system.
            // Intents are fundamental to Android and are basically requests to the Android system
            // or to another application to do some certain action.
            // The intent that we are doing here is an intent to go from the MainActivity and
            // launch the CreateActivity
            val intent = Intent(this, CreateActivity::class.java)
            // There are two parameters in the intent constructor, the first is a context where we
            // are going to pass in "this", referring to where we're coming from
            // The second parameter is the class that we want to navigate to

            // Inside the intent we are going to put an extra and that will be the desired board
            // size
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)

            // Now, in order to actually navigate to the CreateActivity, we need to call this
            // method start activity, which has two versions.
            // One is startActivity and the other is startActivityForResult

            // startActivityForResult is necessary if we want to get some data back from the
            // that you've launched

            // In our case, we're going to be launching the CreateActivity, the user will be
            // creating a new board there and whenever that's done we want to get that data back
            // in the main activity and allow the user to play that custom game that they just
            // created
            // Since we want to get that signal back from our child activity, we will use
            // startActivityForResult

            // Pass in the intent that we just created, since we are using startActivityForResult,
            // we also need to pass in a second parameter, which is the request code
            startActivityForResult(intent, CREATE_REQUEST_CODE)
        })
    }

    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)

        // Define the radio group, i.e. pull it out of the board size view
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)

        // When we open up the dialog, we want the current sized board to be the one that is
        // automatically selected in the dialog
        when (boardSize) {
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }

        // Pass in a list of all the different options of board sizes
        showAlertDialog("Choose new size", boardSizeView, View.OnClickListener {
            // Set a new value for the board size depending on which RadioGroup button was selected
            // The radioGroup will mandate that only one of the buttons inside of it can be selected
            // at a given time
            // Depending on which of those was selected, which is a checked radio button id, we
            // will set the board size accordingly
            boardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            // null out previous data saved inside of game name and custom game images
            gameName = null
            customGameImages = null
            setupBoard()
        })
    }

    private fun showAlertDialog(title: String, view: View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
            // Pass in a title which will be passed in as a parameter
            .setTitle(title)
            .setView(view)
            // By passing null here we are basically saying dismiss the alert dialog if the user
                // taps on "Cancel"
            .setNegativeButton("Cancel", null)
            //we put in underscores to indicate we are still adhering to the declaration but that's
                // the method we're overwriting
            .setPositiveButton("OK") { _, _ ->
                positiveClickListener.onClick(null)
            }.show()
    }

    private fun setupBoard() {
        // If the user is playing a custom game then we should change the title to be the name of
        // the game
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        when (boardSize) {
            BoardSize.EASY -> {
                tvNumMoves.text = "Easy: 4 x 2"
                tvNumPairs.text = "Pairs: 0/ 4"
            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = "Medium: 6 x 3"
                tvNumPairs.text = "Pairs: 0/ 9"
            }
            BoardSize.HARD -> {
                tvNumMoves.text = "Hard: 6 x 4"
                tvNumPairs.text = "Pairs: 0/ 12"
            }
        }

        tvNumPairs.setTextColor(ContextCompat.getColor(this,R.color.color_progress_none))

        memoryGame = MemoryGame(boardSize, customGameImages)

        clRoot = findViewById(R.id.clRoot)

        // Every RecyclerView has two core components: one is the the Adapter and the other is the
        // LayoutManager

        // The Adapter is more involved than the LayoutManager because its responsible for taking
        // in the underlying data set of the RecyclerView and turning that into (or adapting) each
        // piece of data into a view
        adapter = MemoryBoardAdapter(this, boardSize, memoryGame.cards, object: MemoryBoardAdapter.CardClickListener {
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }//this TAG needs a companion object

        })
        rvBoard.adapter = adapter
        // This is optional but is performance optimization:
        rvBoard.setHasFixedSize(true)

        // Given some views that should be shown in the RecyclerView, the Layout Manager is
        // responsible for measuring and positioning those items

        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())
        // built in that gives a grid effect takes in 2 parameters: one is the context, and we are
        // passing in 'this', which is referencing the MainActivity since the MainActivity is an
        // example of a context
        // And the second parameter is the spanCount, otherwise known as how many columns are in
        // the RecyclerView, we will start off by hard coding it and later changing that
    }

    private fun updateGameWithFlip(position: Int) {
        // Error checking for wins
        if (memoryGame.haveWonGame()) {
            // Alert the user, this move isn't valid
                //we need to define the root element on which this Snackbar will be anchored
                Snackbar.make(clRoot, "You already won!", Snackbar.LENGTH_LONG).show()
            return
        }
        // Error checking if the card is already face up
        if (memoryGame.isCardFaceUp(position)) {
            // Alert the user, this move isn't valid
            Snackbar.make(clRoot, "Invalid move!", Snackbar.LENGTH_SHORT).show()
            return
        }
        // Actually flip over the card
        if (memoryGame.flipCard(position)) {
            Log.i(TAG, "Found a match! Number of Pairs Found: ${memoryGame.numPairsFound}")

            // Add colour interpolation on the number of pairs
                // interpolation is a statistacal term
                // For example, if you are walking 1000 steps and are 75% done then I would say you
                // have taken roughly 750 steps, that is linear interpolation
            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                // The second and third parameters are the start and end values representing no
                // progress made and complete progress made
                // We will use ContextCompat to get the colour and we are going to have to actually
                // define the colored resource in our colors.xml file
                ContextCompat.getColor(this,R.color.color_progress_none),
                ContextCompat.getColor(this,R.color.color_progress_full)
            ) as Int
            tvNumPairs.setTextColor(color) // this setTextColor requires and integer so we cast
            // the value of this argb evaluator "as Int"

            // Here we are capturing the return value of memoryGame.flipcard and if it returns true
            // then the user has successfully found a pair of matching memory cards
            // In that case we reference the textView which is tvNumPairs and set the text
            // attribute to show this higher number of pairs found
            tvNumPairs.text = "Pairs: ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
            if (memoryGame.haveWonGame()) {
                Snackbar.make(clRoot, "You won! Congratulations.", Snackbar.LENGTH_LONG).show()
                CommonConfetti.rainingConfetti(clRoot, intArrayOf(Color. YELLOW, Color. GREEN, Color. MAGENTA)).oneShot()
            }
        }
        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()
    }
}