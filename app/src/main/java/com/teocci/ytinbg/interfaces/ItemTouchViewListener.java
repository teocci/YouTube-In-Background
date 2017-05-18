package com.teocci.ytinbg.interfaces;

import android.support.v7.widget.helper.ItemTouchHelper;

/**
 * Interface that notifies an item ViewHolder of relevant callbacks from {@link
 * ItemTouchHelper.Callback}.
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-May-15
 */

public interface ItemTouchViewListener
{
    /**
     * Called when the {@link ItemTouchHelper} first registers an item as being moved or swiped.
     * Implementations should update the item view to indicate it's active state.
     */
    void onItemSelected();


    /**
     * Called when the {@link ItemTouchHelper} has completed the move or swipe, and the active item
     * state should be cleared.
     */
    void onItemClear();
}
