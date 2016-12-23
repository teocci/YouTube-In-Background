package com.teocci.ytinbg.interfaces;

import java.util.ArrayList;

/**
 * Interface for receive the JSON response asynchronously
 * Created by teocci on 12/23/16.
 */

public interface JsonAsyncResponse
{
    void processFinish(ArrayList<String> result);
}
