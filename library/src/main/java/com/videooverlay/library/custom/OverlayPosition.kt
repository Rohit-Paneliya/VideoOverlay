package com.videooverlay.library.custom

sealed class OverlayPosition {
    object TOP_RIGHT: OverlayPosition(){
        override fun toString(): String {
            return "overlay=W-w-5:5"
        }
    }

    object TOP_LEFT: OverlayPosition(){
        override fun toString(): String {
            return "overlay=5:5"
        }
    }

    object BOTTOM_RIGHT: OverlayPosition(){
        override fun toString(): String {
            return "overlay=W-w-5:H-h-5"
        }
    }

    object BOTTOM_LEFT: OverlayPosition(){
        override fun toString(): String {
            return "overlay=5:H-h-5"
        }
    }

    object BOTTOM_CENTER: OverlayPosition(){
        override fun toString(): String {
            return "overlay=x=(W-w)/2:y=H-h-5"
        }
    }

    object CENTER: OverlayPosition(){
        override fun toString(): String {
            return "overlay=(W-w)/2:(H-h)/2"
        }
    }
}