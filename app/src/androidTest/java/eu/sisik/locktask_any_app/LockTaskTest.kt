package eu.sisik.locktask_any_app

import android.app.Activity
import android.app.Instrumentation
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.*
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.hasEntry
import org.hamcrest.TypeSafeMatcher

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class LockTaskTest {

    @get:Rule
    val intentsTestRule = IntentsTestRule(MainActivity::class.java)

    private lateinit var context: Context
    private lateinit var dpm: DevicePolicyManager
    private lateinit var cn: ComponentName

    @Test
    fun appIsDevOwner() {
        ActivityScenario.launch(MainActivity::class.java)
        assertTrue(dpm.isDeviceOwnerApp(context.packageName))
    }

    @Test
    fun selectedPackageStartedInLockTaskMode() {
        val expectedPackageName = "eu.sisik.apktools"
        ActivityScenario.launch(MainActivity::class.java)
        intending(toPackage(expectedPackageName)) // Prevent from actually starting the activity in Lock Task mode
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent()))

        // Select our package name
        onView(withId(R.id.rvPackageList)).perform(actionOnHolderItem(withItemPkgInViewHolder(expectedPackageName), click()))

        // The tapped package name should now be selected
        onView(allOf(withId(R.id.tvPackageName),
            withParent(instanceOf(ConstraintLayout::class.java))))
            .check(matches(withText(containsString(expectedPackageName))))

        // Lock Task not yet allowed for this package
        assertFalse(dpm.isLockTaskPermitted(expectedPackageName))

        // Whitelist package for Lock Task mode
        onView(withId(R.id.butStartLockTask)).perform(click())

        // Package should be whitelisted now
        assertTrue(dpm.isLockTaskPermitted(expectedPackageName))

        // Launch intent for this package should now be sent
        intended(toPackage(expectedPackageName))
    }

    @Test
    fun appIsLockTaskWhitelisted() {
        assertTrue(dpm.isLockTaskPermitted(context.packageName))
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        cn = ComponentName(context, DevAdminReceiver::class.java)

        assertTrue(dpm.isDeviceOwnerApp(context.packageName))

        clearAllLockTaskPackages()
    }

    private fun clearAllLockTaskPackages() {
        dpm.setLockTaskPackages(cn, arrayOf())
    }

    companion object {
        fun withItemPkgInViewHolder(packageName: String): Matcher<RecyclerView.ViewHolder> {
            return object : BoundedMatcher<RecyclerView.ViewHolder, PackageAdapter.ViewHolder>(
                PackageAdapter.ViewHolder::class.java
            ){
                override fun describeTo(description: Description?) {
                    description?.appendText("with item package name " + packageName)
                }

                override fun matchesSafely(item: PackageAdapter.ViewHolder?): Boolean {
                    return item?.tvPackageName?.text == packageName
                }
            }
        }
    }
}
