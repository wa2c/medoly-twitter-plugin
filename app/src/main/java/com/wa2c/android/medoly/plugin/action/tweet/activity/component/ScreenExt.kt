package com.wa2c.android.medoly.plugin.action.tweet.activity.component

import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.wa2c.android.medoly.plugin.action.tweet.R
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <T: ViewDataBinding> Fragment.viewBinding(): ReadOnlyProperty<Fragment, T> {
    return object: ReadOnlyProperty<Fragment, T> {
        override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
            val view = thisRef.requireView()
            @Suppress("UNCHECKED_CAST")
            return (view.getTag(R.id.tag_binding) as? T) ?: let {
                val b = DataBindingUtil.bind<T>(view)!!
                b.lifecycleOwner = thisRef.viewLifecycleOwner
                view.setTag(R.id.tag_binding, b)
                b
            }
        }
    }
}
