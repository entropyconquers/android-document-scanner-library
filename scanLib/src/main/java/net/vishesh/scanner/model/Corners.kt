package net.vishesh.scanner.model

import android.util.Log
import org.opencv.core.Point
import org.opencv.core.Size
import kotlin.math.max


data class Corners(val points: List<Point>, var size: Size) {
    fun log() {

        Log.d(
            javaClass.simpleName,
            "ERROR A size: ${size.width}x${size.height} - tl: ${tl.x}, ${tl.y} - tr: ${tr.x}, ${tr.y} - br: ${br.x}, ${br.y} - bl: ${bl.x}, ${bl.y}"
        )
    }
    fun pointsToPair() : List<Pair<Double, Double>> {
        return listOf(Pair(points[0].x, points[0].y),Pair(points[1].x, points[1].y),Pair(points[2].x, points[2].y),Pair(points[3].x, points[3].y))
    }
    fun calculateSize(): Size {
        return Size(
            max(points[1].x - points[0].x, points[3].x - points[2].x),
            max(points[1].y - points[0].y, points[3].y - points[2].y)
        )
    }
    fun calculateAngle(): Double {
        return Math.atan2(
            points[2].y - points[1].y,
            points[2].x - points[1].x
        )
    }
    fun isTooSteep(): Boolean {
        return Math.abs(points[3].x - points[2].x) > Math.abs(points[1].x - points[0].x)*1.1
    }

    val tl: Point = points[0]
    val tr: Point = points[1]
    val br: Point = points[2]
    val bl: Point = points[3]
}
