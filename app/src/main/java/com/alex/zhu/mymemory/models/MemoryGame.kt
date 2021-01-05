package com.alex.zhu.mymemory.models

import com.alex.zhu.mymemory.utils.DEFAULT_ICONS

class MemoryGame (private val boardSize: BoardSize) {

    //the cards are going to be a member variable in this class
    val cards: List<MemoryCard>
    var numPairsFound = 0
    private var numCardFlips = 0
    // A nullable int because when you make a new memory card game there is no single selected card
    private var indexOfSingleSelectedCard: Int? = null

    init{
        val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
        //shuffled is randomized and we are going to take a certain number. The number of images
        //we take will be boardSize.getNumPairs, since, for example, for an 8 card game we want
        //4 images, or 4 pairs of images, taking 4 images out of the DEFAULT_ICONS

        val randomizedImages = (chosenImages + chosenImages).shuffled()
        //and this randomized list of 4 images, doubled and shuffled will be what we pass into
        //the adapter (added an extra parameter)

        //each randomized Image will correspond to one memory card and we want to create a list of
        //those memory cards
        //we are going to utilize the map function on randomizedImages, what that does is for
        //every element of randomizedImages we are going to do an operation and create a new list
        //We are going to transform randomizedImages into a new list

        //In particular, create a new memoryCard object
        //takes in three parameters that we defined:
        //1 is the identifier and that will be the current randomized image that we are mapping
        //over and we refer to that as it
        //since we gave a default value for isFaceUp and isMatched, we actually don't need to
        //specify it (false here) so we can just pass in one parameter to MemoryCard
        cards = randomizedImages.map{ MemoryCard(it) }
    }
    fun flipCard(position: Int): Boolean {
        numCardFlips++
        val card = cards[position]
        var foundMatch = false
        // Three cases:
        // can copy restore cards into first case making the first and third cases identical and
        // restore cases in the first case is a no-op meaning it has no impact
        // 0 cards previously flipped over => flip over the selected card
        // 1 cards previously flipped over => flip over the selected card + check if the images match
        // 2 cards previously flipped over => restore cards + flip over the selected card
        if (indexOfSingleSelectedCard == null) {
            //0 or 2 cards previously flipped over
            restoreCards()
            indexOfSingleSelectedCard = position
        } else {
            // exactly 1 card previously flipped over
            foundMatch = checkForMatch(indexOfSingleSelectedCard!!, position)
            // in order to force it to be a non-null int, we can use !! to tell the Kotlin compiler
            // to not worry about this
            indexOfSingleSelectedCard = null
        }

        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean {
        // if the identifier of the card at position1 is not equal to the identifier of the card at
        // position2
        if (cards[position1].identifier != cards[position2].identifier) {
            return false
        }

        cards[position1].isMatched = true
        cards[position2].isMatched = true
        numPairsFound++
        return true
    }

    private fun restoreCards() {
        for (card in cards) {
            //if card is not matched
            if (!card.isMatched) {
                card.isFaceUp = false
            }
        }
    }

    fun haveWonGame(): Boolean {
        // is exactly equal to the total number of pairs that should be on this board
        return numPairsFound == boardSize.getNumPairs()
    }

    fun isCardFaceUp(position: Int): Boolean {
        // all we need to do here is grab the card at that position and check the value of
        // isFaceUp
        return cards[position].isFaceUp
    }

    fun getNumMoves(): Int {
        // Here we are doing integer truncation so if the number of card flips is 5 when we do
        // 5 / 2, the result will be 2 because we are rounding down
        return numCardFlips / 2
    }
}