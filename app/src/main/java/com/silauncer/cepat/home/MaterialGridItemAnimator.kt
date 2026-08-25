package com.silauncer.cepat.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.content.Context
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.silauncer.cepat.R

// [app/src/main/java/com/silauncer/cepat/home/MaterialGridItemAnimator.kt]: Material Design Motion ItemAnimator untuk Grid Launcher
// [Penjelasan]: Mengimplementasikan transisi gerak Material Design 3 saat aplikasi ditambahkan, dihapus, atau diorganisasi ulang
class MaterialGridItemAnimator(context: Context? = null) : SimpleItemAnimator() {

    // [app/src/main/java/com/silauncer/cepat/home/MaterialGridItemAnimator.kt]: Kurva interpolasi Material Design 3
    // [Penjelasan]: Menggunakan kurva Emphasized Decelerate, Accelerate, dan Standard untuk pergerakan fluid
    private val interpolatorAdd: TimeInterpolator by lazy { PathInterpolatorCompat.create(0.05f, 0.7f, 0.1f, 1.0f) }
    private val interpolatorRemove: TimeInterpolator by lazy { PathInterpolatorCompat.create(0.3f, 0.0f, 0.8f, 0.15f) }
    private val interpolatorMove: TimeInterpolator by lazy { PathInterpolatorCompat.create(0.2f, 0.0f, 0.0f, 1.0f) }
    private val interpolatorChange: TimeInterpolator by lazy { PathInterpolatorCompat.create(0.4f, 0.0f, 0.2f, 1.0f) }

    private val pendingRemovals = ArrayList<RecyclerView.ViewHolder>()
    private val pendingAdditions = ArrayList<RecyclerView.ViewHolder>()
    private val pendingMoves = ArrayList<MoveInfo>()
    private val pendingChanges = ArrayList<ChangeInfo>()

    private val movesList = ArrayList<ArrayList<MoveInfo>>()
    private val additionsList = ArrayList<ArrayList<RecyclerView.ViewHolder>>()
    private val changesList = ArrayList<ArrayList<ChangeInfo>>()

    private val removeAnimations = ArrayList<RecyclerView.ViewHolder>()
    private val moveAnimations = ArrayList<RecyclerView.ViewHolder>()
    private val addAnimations = ArrayList<RecyclerView.ViewHolder>()
    private val changeAnimations = ArrayList<RecyclerView.ViewHolder>()

    init {
        supportsChangeAnimations = false
        if (context != null) {
            val res = context.resources
            removeDuration = res.getInteger(R.integer.motion_duration_app_remove).toLong()
            addDuration = res.getInteger(R.integer.motion_duration_app_add).toLong()
            moveDuration = res.getInteger(R.integer.motion_duration_app_move).toLong()
            changeDuration = res.getInteger(R.integer.motion_duration_app_change).toLong()
        } else {
            removeDuration = 200L
            addDuration = 280L
            moveDuration = 320L
            changeDuration = 240L
        }
    }

    private data class MoveInfo(
        val holder: RecyclerView.ViewHolder,
        val fromX: Int,
        val fromY: Int,
        val toX: Int,
        val toY: Int
    )

    private data class ChangeInfo(
        var oldHolder: RecyclerView.ViewHolder?,
        var newHolder: RecyclerView.ViewHolder?,
        val fromX: Int,
        val fromY: Int,
        val toX: Int,
        val toY: Int
    )

    override fun runPendingAnimations() {
        val removalsPending = pendingRemovals.isNotEmpty()
        val movesPending = pendingMoves.isNotEmpty()
        val changesPending = pendingChanges.isNotEmpty()
        val additionsPending = pendingAdditions.isNotEmpty()

        if (!removalsPending && !movesPending && !additionsPending && !changesPending) return

        // 1. Eksekusi animasi remove (Material Exit)
        for (holder in pendingRemovals) animateRemoveImpl(holder)
        pendingRemovals.clear()

        // 2. Eksekusi animasi pergeseran / reorganisasi posisi (Material Move)
        if (movesPending) {
            val moves = ArrayList(pendingMoves)
            movesList.add(moves)
            pendingMoves.clear()
            val mover = Runnable {
                for (moveInfo in moves) animateMoveImpl(moveInfo.holder, moveInfo.fromX, moveInfo.fromY, moveInfo.toX, moveInfo.toY)
                moves.clear()
                movesList.remove(moves)
            }
            if (removalsPending) moves[0].holder.itemView.postDelayed(mover, removeDuration) else mover.run()
        }

        // 3. Eksekusi animasi perubahan data jika ada
        if (changesPending) {
            val changes = ArrayList(pendingChanges)
            changesList.add(changes)
            pendingChanges.clear()
            val changer = Runnable {
                for (change in changes) animateChangeImpl(change)
                changes.clear()
                changesList.remove(changes)
            }
            if (removalsPending) {
                val targetView = (changes[0].oldHolder ?: changes[0].newHolder)?.itemView
                targetView?.postDelayed(changer, removeDuration) ?: changer.run()
            } else {
                changer.run()
            }
        }

        // 4. Eksekusi animasi penambahan item baru (Material Enter)
        if (additionsPending) {
            val additions = ArrayList(pendingAdditions)
            additionsList.add(additions)
            pendingAdditions.clear()
            val adder = Runnable {
                for (holder in additions) animateAddImpl(holder)
                additions.clear()
                additionsList.remove(additions)
            }
            if (removalsPending || movesPending || changesPending) {
                val totalDelay = (if (removalsPending) removeDuration else 0L) +
                        (if (movesPending) moveDuration else if (changesPending) changeDuration else 0L)
                additions[0].itemView.postDelayed(adder, totalDelay)
            } else {
                adder.run()
            }
        }
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        resetAnimation(holder)
        pendingRemovals.add(holder)
        return true
    }

    private fun animateRemoveImpl(holder: RecyclerView.ViewHolder) {
        val view = holder.itemView
        val animation = view.animate()
        removeAnimations.add(holder)
        dispatchRemoveStarting(holder)

        animation.setDuration(removeDuration)
            .alpha(0f)
            .scaleX(TRANSITION_SCALE_FACTOR)
            .scaleY(TRANSITION_SCALE_FACTOR)
            .setInterpolator(interpolatorRemove)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animator: Animator) = dispatchRemoveStarting(holder)
                override fun onAnimationEnd(animator: Animator) {
                    animation.setListener(null)
                    view.alpha = 1f
                    view.scaleX = 1f
                    view.scaleY = 1f
                    dispatchRemoveFinished(holder)
                    removeAnimations.remove(holder)
                    dispatchFinishedWhenDone()
                }
            }).start()
    }

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        resetAnimation(holder)
        holder.itemView.alpha = 0f
        holder.itemView.scaleX = TRANSITION_SCALE_FACTOR
        holder.itemView.scaleY = TRANSITION_SCALE_FACTOR
        pendingAdditions.add(holder)
        return true
    }

    private fun animateAddImpl(holder: RecyclerView.ViewHolder) {
        val view = holder.itemView
        val animation = view.animate()
        addAnimations.add(holder)
        dispatchAddStarting(holder)

        animation.setDuration(addDuration)
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setInterpolator(interpolatorAdd)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animator: Animator) = dispatchAddStarting(holder)
                override fun onAnimationEnd(animator: Animator) {
                    animation.setListener(null)
                    view.alpha = 1f
                    view.scaleX = 1f
                    view.scaleY = 1f
                    dispatchAddFinished(holder)
                    addAnimations.remove(holder)
                    dispatchFinishedWhenDone()
                }
            }).start()
    }

    override fun animateMove(holder: RecyclerView.ViewHolder, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
        val view = holder.itemView
        val actualFromX = fromX + view.translationX.toInt()
        val actualFromY = fromY + view.translationY.toInt()
        resetAnimation(holder)
        val deltaX = toX - actualFromX
        val deltaY = toY - actualFromY
        if (deltaX == 0 && deltaY == 0) {
            dispatchMoveFinished(holder)
            return false
        }
        if (deltaX != 0) view.translationX = -deltaX.toFloat()
        if (deltaY != 0) view.translationY = -deltaY.toFloat()
        pendingMoves.add(MoveInfo(holder, actualFromX, actualFromY, toX, toY))
        return true
    }

    private fun animateMoveImpl(holder: RecyclerView.ViewHolder, fromX: Int, fromY: Int, toX: Int, toY: Int) {
        val view = holder.itemView
        val deltaX = toX - fromX
        val deltaY = toY - fromY
        if (deltaX != 0) view.animate().translationX(0f)
        if (deltaY != 0) view.animate().translationY(0f)
        val animation = view.animate()
        moveAnimations.add(holder)
        dispatchMoveStarting(holder)

        animation.setDuration(moveDuration)
            .setInterpolator(interpolatorMove)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animator: Animator) = dispatchMoveStarting(holder)
                override fun onAnimationCancel(animator: Animator) {
                    if (deltaX != 0) view.translationX = 0f
                    if (deltaY != 0) view.translationY = 0f
                }
                override fun onAnimationEnd(animator: Animator) {
                    animation.setListener(null)
                    dispatchMoveFinished(holder)
                    moveAnimations.remove(holder)
                    dispatchFinishedWhenDone()
                }
            }).start()
    }

    override fun animateChange(
        oldHolder: RecyclerView.ViewHolder,
        newHolder: RecyclerView.ViewHolder?,
        fromLeft: Int,
        fromTop: Int,
        toLeft: Int,
        toTop: Int
    ): Boolean {
        if (oldHolder === newHolder) return animateMove(oldHolder, fromLeft, fromTop, toLeft, toTop)
        val prevX = oldHolder.itemView.translationX
        val prevY = oldHolder.itemView.translationY
        val prevAlpha = oldHolder.itemView.alpha
        resetAnimation(oldHolder)
        val deltaX = (toLeft - fromLeft - prevX).toInt()
        val deltaY = (toTop - fromTop - prevY).toInt()
        oldHolder.itemView.translationX = prevX
        oldHolder.itemView.translationY = prevY
        oldHolder.itemView.alpha = prevAlpha
        if (newHolder != null) {
            resetAnimation(newHolder)
            newHolder.itemView.translationX = -deltaX.toFloat()
            newHolder.itemView.translationY = -deltaY.toFloat()
            newHolder.itemView.alpha = 0f
        }
        pendingChanges.add(ChangeInfo(oldHolder, newHolder, fromLeft, fromTop, toLeft, toTop))
        return true
    }

    private fun animateChangeImpl(changeInfo: ChangeInfo) {
        val holder = changeInfo.oldHolder
        val view = holder?.itemView
        val newHolder = changeInfo.newHolder
        val newView = newHolder?.itemView

        if (view != null) {
            val oldAnim = view.animate().setDuration(changeDuration).setInterpolator(interpolatorChange)
            changeAnimations.add(holder)
            oldAnim.translationX((changeInfo.toX - changeInfo.fromX).toFloat())
                .translationY((changeInfo.toY - changeInfo.fromY).toFloat())
                .alpha(0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animator: Animator) = dispatchChangeStarting(holder, true)
                    override fun onAnimationEnd(animator: Animator) {
                        oldAnim.setListener(null)
                        view.alpha = 1f
                        view.translationX = 0f
                        view.translationY = 0f
                        dispatchChangeFinished(holder, true)
                        changeAnimations.remove(holder)
                        dispatchFinishedWhenDone()
                    }
                }).start()
        }

        if (newView != null) {
            val newAnim = newView.animate().setDuration(changeDuration).setInterpolator(interpolatorChange)
            changeAnimations.add(newHolder)
            newAnim.translationX(0f).translationY(0f).alpha(1f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animator: Animator) = dispatchChangeStarting(newHolder, false)
                    override fun onAnimationEnd(animator: Animator) {
                        newAnim.setListener(null)
                        newView.alpha = 1f
                        newView.translationX = 0f
                        newView.translationY = 0f
                        dispatchChangeFinished(newHolder, false)
                        changeAnimations.remove(newHolder)
                        dispatchFinishedWhenDone()
                    }
                }).start()
        }
    }

    override fun endAnimation(item: RecyclerView.ViewHolder) {
        val view = item.itemView
        view.animate().cancel()

        for (i in pendingMoves.indices.reversed()) {
            if (pendingMoves[i].holder === item) {
                view.translationY = 0f
                view.translationX = 0f
                dispatchMoveFinished(item)
                pendingMoves.removeAt(i)
            }
        }
        endChangeAnimation(pendingChanges, item)
        if (pendingRemovals.remove(item)) {
            view.alpha = 1f
            view.scaleX = 1f
            view.scaleY = 1f
            dispatchRemoveFinished(item)
        }
        if (pendingAdditions.remove(item)) {
            view.alpha = 1f
            view.scaleX = 1f
            view.scaleY = 1f
            dispatchAddFinished(item)
        }

        for (i in changesList.indices.reversed()) {
            val changes = changesList[i]
            endChangeAnimation(changes, item)
            if (changes.isEmpty()) changesList.removeAt(i)
        }
        for (i in movesList.indices.reversed()) {
            val moves = movesList[i]
            for (j in moves.indices.reversed()) {
                if (moves[j].holder === item) {
                    view.translationY = 0f
                    view.translationX = 0f
                    dispatchMoveFinished(item)
                    moves.removeAt(j)
                    if (moves.isEmpty()) movesList.removeAt(i)
                    break
                }
            }
        }
        for (i in additionsList.indices.reversed()) {
            val additions = additionsList[i]
            if (additions.remove(item)) {
                view.alpha = 1f
                view.scaleX = 1f
                view.scaleY = 1f
                dispatchAddFinished(item)
                if (additions.isEmpty()) additionsList.removeAt(i)
            }
        }
        dispatchFinishedWhenDone()
    }

    private fun resetAnimation(holder: RecyclerView.ViewHolder) {
        holder.itemView.animate().cancel()
        endAnimation(holder)
    }

    override fun endAnimations() {
        for (i in pendingMoves.indices.reversed()) {
            val item = pendingMoves[i]
            item.holder.itemView.translationY = 0f
            item.holder.itemView.translationX = 0f
            dispatchMoveFinished(item.holder)
            pendingMoves.removeAt(i)
        }
        for (i in pendingRemovals.indices.reversed()) dispatchRemoveFinished(pendingRemovals.removeAt(i))
        for (i in pendingAdditions.indices.reversed()) {
            val item = pendingAdditions.removeAt(i)
            item.itemView.alpha = 1f
            item.itemView.scaleX = 1f
            item.itemView.scaleY = 1f
            dispatchAddFinished(item)
        }
        for (i in pendingChanges.indices.reversed()) endChangeAnimationIfNecessary(pendingChanges[i])
        pendingChanges.clear()
        if (!isRunning) return

        for (i in movesList.indices.reversed()) {
            val moves = movesList[i]
            for (j in moves.indices.reversed()) {
                val item = moves[j].holder
                item.itemView.translationY = 0f
                item.itemView.translationX = 0f
                dispatchMoveFinished(item)
                moves.removeAt(j)
            }
            movesList.removeAt(i)
        }
        for (i in additionsList.indices.reversed()) {
            val additions = additionsList[i]
            for (j in additions.indices.reversed()) {
                val item = additions[j]
                item.itemView.alpha = 1f
                item.itemView.scaleX = 1f
                item.itemView.scaleY = 1f
                dispatchAddFinished(item)
            }
            additionsList.removeAt(i)
        }
        for (i in changesList.indices.reversed()) {
            val changes = changesList[i]
            for (j in changes.indices.reversed()) endChangeAnimationIfNecessary(changes[j])
            changesList.removeAt(i)
        }

        cancelAll(removeAnimations)
        cancelAll(moveAnimations)
        cancelAll(addAnimations)
        cancelAll(changeAnimations)
        dispatchAnimationsFinished()
    }

    private fun cancelAll(viewHolders: List<RecyclerView.ViewHolder>) {
        for (i in viewHolders.indices.reversed()) viewHolders[i].itemView.animate().cancel()
    }

    private fun endChangeAnimation(infoList: MutableList<ChangeInfo>, item: RecyclerView.ViewHolder) {
        for (i in infoList.indices.reversed()) {
            val changeInfo = infoList[i]
            if (endChangeAnimationIfNecessary(changeInfo, item)) {
                if (changeInfo.oldHolder == null && changeInfo.newHolder == null) infoList.remove(changeInfo)
            }
        }
    }

    private fun endChangeAnimationIfNecessary(changeInfo: ChangeInfo) {
        changeInfo.oldHolder?.let { endChangeAnimationIfNecessary(changeInfo, it) }
        changeInfo.newHolder?.let { endChangeAnimationIfNecessary(changeInfo, it) }
    }

    private fun endChangeAnimationIfNecessary(changeInfo: ChangeInfo, item: RecyclerView.ViewHolder): Boolean {
        var oldItem = false
        if (changeInfo.newHolder === item) {
            changeInfo.newHolder = null
        } else if (changeInfo.oldHolder === item) {
            changeInfo.oldHolder = null
            oldItem = true
        } else {
            return false
        }
        item.itemView.alpha = 1f
        item.itemView.translationX = 0f
        item.itemView.translationY = 0f
        dispatchChangeFinished(item, oldItem)
        return true
    }

    override fun isRunning(): Boolean {
        return (pendingAdditions.isNotEmpty() || pendingChanges.isNotEmpty() ||
                pendingMoves.isNotEmpty() || pendingRemovals.isNotEmpty() ||
                moveAnimations.isNotEmpty() || removeAnimations.isNotEmpty() ||
                addAnimations.isNotEmpty() || changeAnimations.isNotEmpty() ||
                movesList.isNotEmpty() || additionsList.isNotEmpty() || changesList.isNotEmpty())
    }

    private fun dispatchFinishedWhenDone() {
        if (!isRunning) dispatchAnimationsFinished()
    }

    companion object {
        // [app/src/main/java/com/silauncer/cepat/home/MaterialGridItemAnimator.kt]: Faktor skala transisi
        // [Penjelasan]: Skala awal item saat muncul (0.75x) dan skala akhir saat dihapus
        private const val TRANSITION_SCALE_FACTOR = 0.75f
    }
}
