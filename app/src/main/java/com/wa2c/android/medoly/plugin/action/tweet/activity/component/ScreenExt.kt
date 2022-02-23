package com.wa2c.android.medoly.plugin.action.tweet.activity.component

import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <T : ViewDataBinding> AppCompatActivity.viewBinding(): ReadOnlyProperty<AppCompatActivity, T?> {
    return ReadOnlyProperty<AppCompatActivity, T?> { thisRef, _ ->
        val view = thisRef.findViewById<ViewGroup>(android.R.id.content)[0]
        DataBindingUtil.bind<T>(view)?.also {
            it.lifecycleOwner = thisRef
        }
    }
}

fun <T : ViewDataBinding> Fragment.viewBinding(): ReadOnlyProperty<Fragment, T?> {
    return object : ReadOnlyProperty<Fragment, T?> {
        override fun getValue(thisRef: Fragment, property: KProperty<*>): T? {
            val view = thisRef.view ?: return null
            return DataBindingUtil.bind<T>(view)?.also {
                it.lifecycleOwner = thisRef.viewLifecycleOwner
            }
        }
    }
}
