package com.example.face_mesh_app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class GazeAccessibilityService : AccessibilityService() {
    companion object {
        private var instance: GazeAccessibilityService? = null
        
        fun getInstance(): GazeAccessibilityService? = instance
        
        fun performTap(x: Float, y: Float) {
            instance?.performTapInternal(x, y)
        }
        
        fun performSwipe(direction: String) {
            instance?.performSwipeInternal(direction)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("GazeAccessibility", "Accessibility service connected")
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or 
                        AccessibilityEvent.TYPE_VIEW_FOCUSED or
                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        serviceInfo = info
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d("GazeAccessibility", "Accessibility service destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to handle events, just perform actions
    }

    override fun onInterrupt() {
        Log.d("GazeAccessibility", "Accessibility service interrupted")
    }

    private fun performTapInternal(x: Float, y: Float) {
        try {
            // Find the root node
            val rootNode = rootInActiveWindow ?: return
            
            // Find the node at the specified coordinates
            val targetNode = findNodeAt(rootNode, x.toInt(), y.toInt())
            
            if (targetNode != null) {
                // Try to perform click on the specific node
                val rect = Rect()
                targetNode.getBoundsInScreen(rect)
                
                if (rect.contains(x.toInt(), y.toInt())) {
                    if (targetNode.isClickable) {
                        targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Log.d("GazeAccessibility", "Clicked on node at ($x, $y)")
                    } else {
                        // If node is not clickable, try to find a clickable parent
                        performClickOnClickableParent(targetNode)
                    }
                }
            } else {
                // Fallback: perform global action
                performGlobalAction(GLOBAL_ACTION_BACK)
                Log.d("GazeAccessibility", "Performed global action at ($x, $y)")
            }
        } catch (e: Exception) {
            Log.e("GazeAccessibility", "Error performing tap: ${e.message}")
        }
    }

    private fun findNodeAt(node: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        
        if (rect.contains(x, y)) {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    val result = findNodeAt(child, x, y)
                    if (result != null) {
                        return result
                    }
                }
            }
            return node
        }
        return null
    }

    private fun performClickOnClickableParent(node: AccessibilityNodeInfo) {
        var currentNode = node
        while (currentNode != null) {
            if (currentNode.isClickable) {
                currentNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d("GazeAccessibility", "Clicked on clickable parent")
                break
            }
            currentNode = currentNode.parent
        }
    }

    private fun performSwipeInternal(direction: String) {
        try {
            when (direction) {
                "left" -> performGlobalAction(GLOBAL_ACTION_BACK)
                "right" -> performGlobalAction(GLOBAL_ACTION_HOME)
                "up" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                "down" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            }
            Log.d("GazeAccessibility", "Performed swipe: $direction")
        } catch (e: Exception) {
            Log.e("GazeAccessibility", "Error performing swipe: ${e.message}")
        }
    }
}
