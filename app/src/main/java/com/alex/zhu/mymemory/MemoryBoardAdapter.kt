package com.alex.zhu.mymemory

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.alex.zhu.mymemory.models.BoardSize
import com.alex.zhu.mymemory.models.MemoryCard
import com.squareup.picasso.Picasso
import kotlin.math.min //to use min in onCreateViewHolder function

class MemoryBoardAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val cards: List<MemoryCard>,
    //we have to make them private val so we can reference it
    private val cardClickListener: CardClickListener
) :
    RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

    //we will use a companion object to define our constants starting with _ that is to be put
    //into onCreateViewHolder
    //in Kotlin, companion objects are singletons where we'll define constants we can access its
    //members directly through the containing class
    //think of companion objects similar to static variables in java
    companion object {
        private const val MARGIN_SIZE = 10
        private const val TAG = "MemoryBoardAdapter"
    }

    //the reason we are defining this interface is because whoever constructs the
    //MemoryBoardAdapter, it will be their responsibility to now pass in an instance of
    //this interface,
    //so we will add another variable in the constructor called cardClickListener
    interface CardClickListener {
        fun onCardClicked(position:Int)
    }
    //responsible for figuring out how to create one view of our RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardWidth = parent.width / boardSize.getWidth() - (2 * MARGIN_SIZE)
        val cardHeight = parent.height / boardSize.getHeight() - (2 * MARGIN_SIZE)
        val cardSideLength = min(cardWidth, cardHeight)

        val view = LayoutInflater.from(context).inflate(R.layout.memory_card, parent, false)
        //inflate a resource file that defines our layout

        //now to grab out the CardView from the view that we've inflated and set the width and height
        //of that CardView to be cardSideLength we do:
        val layoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        //pull out the CardView which we gave an id of cardView and we are going to get a reference
        // to the layout params
        //On these layoutParams, this is what will allow us to change the width and height

        //to implement the margins, we have to cast the layout params as a special type of layoutParams
        // called Margin, LayoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength

        layoutParams.setMargins(MARGIN_SIZE,MARGIN_SIZE,MARGIN_SIZE,MARGIN_SIZE)
        return ViewHolder (view)
        //return the view wrapped inside a ViewHolder
    }

    //responsible for taking the data which is at position and binding it to the viewHolder which is
    //also passed in
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    //how many element are in our RecyclerView and that will simply be numPieces
    // (the constructor parameter that we passed in)
    override fun getItemCount() = boardSize.numCards
    //boardSize.numCards is the total number of elements in our memory game

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)

        fun bind(position: Int) {
            val memoryCard = cards[position]

            //if the memoryCard is face up then we will use that as the image, otherwise
            //we'll use the background
            if (memoryCard.isFaceUp) {
                if (memoryCard.imageUrl != null) {
                    Picasso.get().load(memoryCard.imageUrl).into(imageButton)
                } else {
                    imageButton.setImageResource(memoryCard.identifier)
                }
            } else {
                imageButton.setImageResource(R.drawable.ic_launcher_background)
            }



            // in the bind method, based on whether the memory card is match or not, we will set
            // the alpha property of the imageButton view
            // lower opacity, default full bleed opacity
            imageButton.alpha = if (memoryCard.isMatched) .4f else 1.0f
            val colorStateList = if (memoryCard.isMatched) ContextCompat.getColorStateList(context, R.color.color_brightgreen) else null
            ViewCompat.setBackgroundTintList(imageButton,colorStateList)

            // the idea here is that the setBackgroundTintList is a way to set a background or
            // shading to the image button and if the image is matched, we are going to have a
            // gray background which we create using ContextCompat.getColorStateList with the gray
            // colour

            //here is where we are getting notified of a click on an image button and we would
            //like to notify the main activity of this click so that the main activity can tell the
            //memory game class that the user has taken some action and we should update the
            //state appropriately
            //the standard pattern for doing this is to define an interface
            imageButton.setOnClickListener {
                Log.i(TAG, "Clicked on position $position")
                cardClickListener.onCardClicked(position)
            }

        }
    }

}
