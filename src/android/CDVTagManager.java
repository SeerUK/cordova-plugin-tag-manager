/**
 * Copyright (c) 2014 Jared Dickson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.jareddickson.cordova.tagmanager;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;

import com.google.analytics.tracking.android.GAServiceManager;
import com.google.tagmanager.Container;
import com.google.tagmanager.ContainerOpener;
import com.google.tagmanager.ContainerOpener.OpenType;
import com.google.tagmanager.DataLayer;
import com.google.tagmanager.TagManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class CDVTagManager extends CordovaPlugin {

    private Container mContainer;
    private boolean inited = false;

    public CDVTagManager() {
    }

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callback) {
        if (action.equals("initGTM")) {
            return this.initGTM(args, callback);
        } else if (action.equals("exitGTM")) {
            return this.exitGTM(args, callback);
        } else if (action.equals("trackEvent")) {
            return this.trackEvent(args, callback);
        } else if (action.equals("trackPage")) {
            return this.trackPage(args, callback);
        } else if (action.equals("dispatch")) {
            return this.dispatch(args, callback);
        }

        return false;
    }

    protected boolean initGTM(JSONArray args, CallbackContext callback) {
        try {
            // Set the dispatch interval
            GAServiceManager
                .getInstance()
                .setLocalDispatchPeriod(args.getInt(1));

            TagManager tagManager = TagManager
                .getInstance(this.cordova.getActivity().getApplicationContext());

            ContainerOpener.openContainer(
                    tagManager,                             // TagManager instance.
                    args.getString(0),                      // Tag Manager Container ID.
                    OpenType.PREFER_NON_DEFAULT,            // Prefer not to get the default container, but stale is OK.
                    null,                                   // Time to wait for saved container to load (ms). Default is 2000ms.
                    new ContainerOpener.Notifier() {        // Called when container loads.
                        @Override
                        public void containerAvailable(Container container) {
                            // Handle assignment in callback to avoid blocking main thread.
                            mContainer = container;
                            inited = true;
                        }
                    }
            );

            callback.success(
                "initGTM - id = " + args.getString(0) + "; " +
                "interval = " + args.getInt(1) + " seconds"
            );

            return true;
        } catch (final Exception e) {
            callback.error(e.getMessage());
            return false;
        }
    }

    protected boolean exitGTM(JSONArray args, CallbackContext callback) {
        try {
            inited = false;
            callback.success("exitGTM");
            return true;
        } catch (final Exception e) {
            callback.error(e.getMessage());
            return false;
        }
    }

    protected boolean dispatch(JSONArray args, CallbackContext callback) {
        if (!inited) {
            callback.error("dispatch failed - not initialized");
            return false;
        }

        try {
            GAServiceManager.getInstance().dispatchLocalHits();
            callback.success("dispatch sent");
            return true;
        } catch (final Exception e) {
            callback.error(e.getMessage());
            return false;
        }
    }

    protected boolean trackEvent(JSONArray args, CallbackContext callback) {
        if (!inited) {
            callback.error("trackEvent failed - not initialized");
            return false;
        }

        try {
            DataLayer dataLayer = TagManager
                .getInstance(this.cordova.getActivity().getApplicationContext())
                .getDataLayer();

            int eventValue = args.optInt(3, 0);
            String category = args.getString(0);
            String eventAction = args.getString(1);
            String eventLabel = args.getString(2);
            String userId = args.optString(4, null);

            if (userId != null) {
                dataLayer.push(DataLayer.mapOf(
                    "event"             , "interaction",
                    "target"            , category,
                    "action"            , eventAction,
                    "target-properties" , eventLabel,
                    "value"             , eventValue,
                    "user-id"           , userId
                ));

                callback.success(
                    "trackEvent - " +
                    "category = " + category + "; " +
                    "action = " + eventAction + "; " +
                    "label = " + eventLabel + "; " +
                    "value = " + eventValue + "; " +
                    "userId = " + userId
                );
            } else {
                dataLayer.push(DataLayer.mapOf(
                    "event"             , "interaction",
                    "target"            , category,
                    "action"            , eventAction,
                    "target-properties" , eventLabel,
                    "value"             , eventValue
                ));

                callback.success(
                    "trackEvent - " +
                    "category = " + category + "; " +
                    "action = " + eventAction + "; " +
                    "label = " + eventLabel + "; " +
                    "value = " + eventValue
                );
            }

            return true;
        } catch (final Exception e) {
            callback.error(e.getMessage());
            return false;
        }
    }

    protected boolean trackPage(JSONArray args, CallbackContext callback) {
        if (!inited) {
            callback.error("trackPage failed - not initialized");
            return false;
        }

        try {
            DataLayer dataLayer = TagManager
                .getInstance(this.cordova.getActivity().getApplicationContext())
                .getDataLayer();

            String pageURL = args.getString(0);
            String userId = args.optString(1, null);

            if (userId != null) {
                dataLayer.push(DataLayer.mapOf(
                    "event"             , "content-view",
                    "content-name"      , pageURL,
                    "user-id"           , userId
                ));

                callback.success(
                    "trackPage - " +
                    "url = " + pageURL + "; " +
                    "userId = " + userId
                );
            } else {
                dataLayer.push(DataLayer.mapOf(
                    "event"             , "content-view",
                    "content-name"      , pageURL
                ));

                callback.success(
                    "trackPage - " +
                    "url = " + pageURL
                );
            }

            return true;
        } catch (final Exception e) {
            callback.error(e.getMessage());
            return false;
        }
    }
}
