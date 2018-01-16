package com.wa2c.android.medoly.plugin.action.tweet.service

/**
 * Command result.
 */
internal enum class CommandResult {
    /** Succeeded.  */
    SUCCEEDED,
    /** Failed.  */
    FAILED,
    /** Authorization failed.  */
    AUTH_FAILED,
    /** No media.  */
    NO_MEDIA,
    /** Post saved.  */
    SAVED,
    /** Ignore.  */
    IGNORE
}