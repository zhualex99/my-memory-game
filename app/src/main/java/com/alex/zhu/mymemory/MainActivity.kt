package com.alex.zhu.mymemory

import android.animation.ArgbEvaluator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alex.zhu.mymemory.models.BoardSize
import com.alex.zhu.mymemory.models.MemoryCard
import com.alex.zhu.mymemory.models.MemoryGame
import com.alex.zhu.mymemory.utils.DEFAULT_ICONS
import com.google.android.material.snackbar.Snackbar
import java.util.stream.DoubleStream.builder
import java.util.stream.IntStream.builder
import java.util.stream.LongStream.builder

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
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

    private lateinit var adapter: MemoryBoardAdapter

    private lateinit var clRoot: ConstraintLayout

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
        }
        return super.onOptionsItemSelected(item)
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

        memoryGame = MemoryGame(boardSize)

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
            }
        }
        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()
    }
}