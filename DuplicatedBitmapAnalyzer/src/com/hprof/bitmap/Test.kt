package com.hprof.bitmap

import com.hprof.bitmap.DuplicatedBitmapHelper.getDuplicatedBitmap

fun main() {
    val collectInfo = getDuplicatedBitmap("./myhprof.hprof")
    for (info in collectInfo!!) {
        System.out.println(info.string())
    }
}
class Test {
}