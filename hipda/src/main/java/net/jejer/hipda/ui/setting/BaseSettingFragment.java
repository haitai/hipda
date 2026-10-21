package net.jejer.hipda.ui.setting;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import net.jejer.hipda.utils.BackgroundHelper;
import net.jejer.hipda.utils.ColorHelper;
import net.jejer.hipda.utils.Utils;

import java.util.HashSet;
import java.util.Set;

/**
 * base setting fragment
 * Created by GreenSkinMonster on 2015-09-11.
 */
public class BaseSettingFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            if (!BackgroundHelper.applyTo(view, getActivity()))
                view.setBackgroundColor(ColorHelper.getWindowBackgroundColor(getActivity()));
        }
        return view;
    }

    protected void setActionBarTitle(CharSequence title) {
        if (getActivity() != null) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            String t = Utils.nullToText(title);
            if (actionBar != null && !t.equals(actionBar.getTitle())) {
                actionBar.setTitle(t);
            }
        }
    }

    void setActionBarTitle(@StringRes int resId) {
        if (getActivity() != null) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null)
                actionBar.setTitle(resId);
        }
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {

            String stringValue = value.toString();
            if (preference instanceof MultiSelectListPreference) {
                MultiSelectListPreference listPreference = (MultiSelectListPreference) preference;
                Set<String> selectedValues = (Set<String>) value;
                CharSequence[] entries = listPreference.getEntries();
                CharSequence[] entryValues = listPreference.getEntryValues();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < entryValues.length; i++) {
                    String v = entryValues[i].toString();
                    if (selectedValues.contains(v)) {
                        if (sb.length() > 0)
                            sb.append(", ");
                        int index = listPreference.findIndexOfValue(v);
                        sb.append(entries[index]);
                    }
                }
                setSafeSummary(preference, sb.toString());
            } else if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                setSafeSummary(preference,
                        index >= 0 ? listPreference.getEntries()[index]
                                : null);
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                setSafeSummary(preference, TextUtils.isEmpty(stringValue) ? "无" : stringValue);
            }

            return true;
        }
    };

    /**
     * Set the summary of a preference in a way that is safe for ListPreference.
     * <p>
     * ListPreference keeps the text given to setSummary() and, when the summary is
     * read, passes it to {@link String#format(String, Object...)}. A summary
     * containing a half width percent sign is therefore parsed as a format
     * marker and throws {@link java.util.UnknownFormatConversionException},
     * which crashes the settings screen. Escaping the percent sign keeps the
     * text intact while making it a valid format string.
     *
     * @param preference the preference to update
     * @param summary    the text to display, may be null
     */
    protected static void setSafeSummary(Preference preference, CharSequence summary) {
        if (preference == null)
            return;
        if (summary == null) {
            preference.setSummary(null);
            return;
        }
        if (preference instanceof ListPreference)
            preference.setSummary(summary.toString().replace("%", "%%"));
        else
            preference.setSummary(summary);
    }

    protected static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        if (preference == null)
            return;
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        if (preference instanceof CheckBoxPreference) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(
                    preference,
                    PreferenceManager.getDefaultSharedPreferences(
                            preference.getContext()).getBoolean(preference.getKey(),
                            false));
        } else if (preference instanceof SwitchPreference) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(
                    preference,
                    PreferenceManager.getDefaultSharedPreferences(
                            preference.getContext()).getBoolean(preference.getKey(),
                            false));
        } else if (preference instanceof MultiSelectListPreference) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(
                    preference,
                    PreferenceManager.getDefaultSharedPreferences(
                            preference.getContext()).getStringSet(preference.getKey(),
                            new HashSet<String>()));
        } else {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(
                    preference,
                    PreferenceManager.getDefaultSharedPreferences(
                            preference.getContext()).getString(preference.getKey(),
                            ""));
        }
    }

}
