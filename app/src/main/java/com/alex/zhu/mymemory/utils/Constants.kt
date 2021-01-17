package com.alex.zhu.mymemory.utils

import com.alex.zhu.mymemory.R

// The reason we are defining EXTRA_BOARD_SIZE in a seperate file that MainActivity (where I first
// used it) is because we're actually going to be referencing EXTRA_BOARD_SIZE in CreateActivity, so
// for a shared constant between multiple files I'm going to put that into Constants.kt rather than
// defining it into a single file
const val EXTRA_BOARD_SIZE = "EXTRA_BOARD_SIZE"

const val EXTRA_GAME_NAME = "EXTRA_GAME_NAME"

val DEFAULT_ICONS = listOf(
    R.drawable.ic_face,
    R.drawable.ic_flower,
    R.drawable.ic_gift,
    R.drawable.ic_heart,
    R.drawable.ic_home,
    R.drawable.ic_lightning,
    R.drawable.ic_moon,
    R.drawable.ic_plane,
    R.drawable.ic_school,
    R.drawable.ic_send,
    R.drawable.ic_star,
    R.drawable.ic_work
)

