package com.alex.zhu.mymemory

import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.alex.zhu.mymemory.models.BoardSize
import kotlin.math.min

class ImagePickerAdapter(
    private val context: Context,
    private val imageUris: List<Uri>,
    private val boardSize: BoardSize,
    // The final parameter of the adapter is going to be an instance of the ImageClickListener class
    private val imageClickListener: ImageClickListener

    ) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    // Define an interface with one method indicating that the user has tapped on this element
    // and that will be how the CreateActivity gets informed of a click and the CreateActivity
    // is what will launch the flow for allowing the user to pick a photo from their device
    interface ImageClickListener {
        // Exactly one function inside this interface
        fun onPlaceHolderClicked()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_image, parent, false)
        // The width of the parent (the RecyclerView), which is how much space we've allocated for
        // the RecyclerView and partition that up into however many images the user should pick
        // across
        val cardWidth = parent.width / boardSize.getWidth()
        // Divided by the number of rows in our board
        val cardHeight = parent.height / boardSize.getHeight()
        val cardSideLength = min(cardWidth, cardHeight)

        // Grab a reference to the image by using the id
        // view: the LinearLayabout plus the ImageView
        val layoutParams = view.findViewById<ImageView>(R.id.ivCustomImage).layoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        // The return value of this onCreateViewHolder is a ViewHolder
        // Construct this new ViewHolder class and pass in the view that we inflated
        return ViewHolder(view)
    }

    // The number of images that the user has to pick is the number of pairs in this game
    // This is why we passed in boardSize as a constructor parameter
    override fun getItemCount() = boardSize.getNumPairs()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // The intention here is, given a particular position, we want to define "how do we
        // display the UI
        // There are two cases here: if we think back to the second constructor parameter, the list
        // of image URI's, if the position we are binding here is less than the size of the image
        // URI's, that means that the user has picked an image for that position
        // On the other hand, if the position is larger than the size of image URI's then we should
        // just show the default gray background to indicate to the user that they still need to
        // pick an image
        if (position < imageUris.size) {
            // We should show the image selected in the image view so we will delegate the work
            // there to be in this bind method
            holder.bind(imageUris[position])
        } else {
            holder.bind()
        }
    }

    // Just like before, we would like to define an inner class ViewHolder and that will be what
    // we parameterize the constructor of the imagePickerAdapter by
    // Inherits from RecyclerView ViewHolder
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // The ViewHolder is wrapping the custom view that we defined in card_image.xml so we will
        // grab a reference to the image view
        private val ivCustomImage = itemView.findViewById<ImageView>(R.id.ivCustomImage)

        fun bind(uri: Uri) {
            // Now that we have a reference to the ImageView, in this version of the bind method,
            // which takes in a URI, all we need to do is set the image URI on the ivCustomImage
            ivCustomImage.setImageURI(uri)
            // Secondly, we set a null click listener, meaning we don't want to respond to
            // clicks on this image view, so there is no edit functionality (my product decision)
            ivCustomImage.setOnClickListener(null)
        }

        fun bind() {
            // This is where we actually want to be able to listen for the user tapping on this
            // image view because that will be an indication that they want to choose the image
            ivCustomImage.setOnClickListener{
                // Launch intent for user to select photos
                // In the ViewHolder when we bind the setOnClickListener on the ImageView, we are
                // going to invoke the onClickListener method of the interface
                imageClickListener.onPlaceHolderClicked()
            }
        }

    }
}

