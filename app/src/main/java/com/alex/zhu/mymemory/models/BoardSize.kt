package com.alex.zhu.mymemory.models

//made it an enum
enum class BoardSize (val numCards: Int){
    EASY(8),
    MEDIUM(18),
    HARD(24);

    fun getWidth(): Int {
        //return, using the when construct, this, which is referring to the board size on which
        //we're operating (easy, medium or hard)
        //the when expression is similar to a switch statement so it'll evaluate a list of conditions
        //and it will return when the first one is met
        return when (this){
            EASY -> 2
            MEDIUM -> 3
            HARD -> 4
        }
    }

    fun getHeight(): Int {
        return numCards / getWidth()
    }

    fun getNumPairs(): Int {
        return numCards / 2
    }
}