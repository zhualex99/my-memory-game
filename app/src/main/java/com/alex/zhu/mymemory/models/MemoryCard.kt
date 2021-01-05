package com.alex.zhu.mymemory.models

data class MemoryCard (
    //identifier represents the uniqueness of the memory icon which is the underlying resource id
    //integer of the memory card
    val identifier: Int,
    //the second attribute of a memory card is whether it's face up or face down, so we are going
    //to identify another attribute called is face up which will be a boolean
    var isFaceUp: Boolean = false,

    //THERE IS AN IMPORTANT DISTINCTION BETWEEN A VAL AND A VAR: A val is something which, once
    //its set, the value of it can't be changed, whereas var mean that the value can be changed

    var isMatched: Boolean = false
)

//this is our data class