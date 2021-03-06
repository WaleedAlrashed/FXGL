/*
 * FXGL - JavaFX Game Library. The MIT License (MIT).
 * Copyright (c) AlmasB (almaslvl@gmail.com).
 * See LICENSE for details.
 */

package com.almasb.fxgl.gameplay.notification

import com.almasb.fxgl.app.FXGL
import com.almasb.fxgl.core.reflect.ReflectionUtils
import com.almasb.fxgl.ui.Position
import javafx.scene.paint.Color
import javafx.util.Duration
import java.util.*

/**
 * TODO: when we have superstate, use that for adding the notificationView
 * and scheduling timer events
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class NotificationServiceProvider : NotificationService {

    private val ANIMATION_DURATION = Duration.seconds(1.0)
    private val NOTIFICATION_DURATION = Duration.seconds(3.0)

    private val gameScene by lazy { FXGL.getApp().gameScene }

    private val notificationView by lazy { ReflectionUtils.newInstance(FXGL.getSettings().notificationViewFactory) }

    private val queue = ArrayDeque<Notification>()

    override fun getPosition() = notificationView.position

    override fun setPosition(position: Position) {
        notificationView.position = position
    }

    override fun getBackgroundColor() = notificationView.backgroundColor

    override fun setBackgroundColor(backgroundColor: Color) {
        notificationView.backgroundColor = backgroundColor
    }

    override fun getTextColor(): Color = notificationView.textColor

    override fun setTextColor(textColor: Color) {
        notificationView.textColor = textColor
    }

    private var showing = false

    /**
     * Shows a notification with given text.
     * Only 1 notification can be shown at a time.
     * If a notification is being shown already, next notifications
     * will be queued to be shown as soon as space available.
     *
     * @param text the text to show
     */
    override fun pushNotification(text: String) {
        val notification = Notification(text)

        if (showing) {
            queue.add(notification)
        } else {
            showFirstNotification()
            queue.add(notification)
        }
    }

    private fun nextNotification() {
        if (queue.isNotEmpty()) {
            val n = queue.poll()
            notificationView.push(n)

            fireAndScheduleNextNotification(n)
        } else {
            notificationView.playOutAnimation()

            FXGL.getMasterTimer().runOnceAfter(Runnable {
                checkLastPop()
            }, ANIMATION_DURATION)
        }
    }

    private fun checkLastPop() {
        if (queue.isEmpty()) {
            gameScene.removeUINode(notificationView)
            showing = false
        } else {
            // play in animation
            notificationView.playInAnimation()

            FXGL.getMasterTimer().runOnceAfter(Runnable {
                nextNotification()
            }, ANIMATION_DURATION)
        }
    }

    private fun showFirstNotification() {
        showing = true
        gameScene.addUINode(notificationView)

        // play in animation
        notificationView.playInAnimation()

        FXGL.getMasterTimer().runOnceAfter(Runnable {
            nextNotification()
        }, ANIMATION_DURATION)
    }

    private fun fireAndScheduleNextNotification(notification: Notification) {
        FXGL.getEventBus().fireEvent(NotificationEvent(notification))

        // schedule next
        FXGL.getMasterTimer().runOnceAfter(Runnable {
            nextNotification()
        }, NOTIFICATION_DURATION)
    }
}