package cl.monsoon.s1next.singleton;

import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.signature.StringSignature;

import java.util.concurrent.Callable;

import cl.monsoon.s1next.App;
import cl.monsoon.s1next.R;
import cl.monsoon.s1next.fragment.DownloadPreferenceFragment;
import cl.monsoon.s1next.fragment.MainPreferenceFragment;
import cl.monsoon.s1next.util.ColorUtil;
import cl.monsoon.s1next.util.DateUtil;

public final class Settings {

    public enum General {
        INSTANCE;

        private volatile boolean hasWifi;
        private volatile float textScale;

        public static void setWifi(boolean hasWifi) {
            INSTANCE.hasWifi = hasWifi;
        }

        public static float getTextScale() {
            return INSTANCE.textScale;
        }

        public static void setTextScale(SharedPreferences sharedPreferences) {
            String value = getSharedPreferencesString(
                    sharedPreferences,
                    MainPreferenceFragment.PREF_KEY_FONT_SIZE,
                    R.string.pref_font_size_default_value);

            INSTANCE.textScale = TextScale.get(Integer.parseInt(value)).getSize();
        }

        private enum TextScale {

            VERY_SMALL(0.8f), SMALL(0.9f), MEDIUM(1f), LARGE(1.1f), VERY_LARGE(1.2f);

            private static final TextScale[] VALUES = TextScale.values();

            private final float size;

            TextScale(float size) {
                this.size = size;
            }

            public static TextScale get(int i) {
                return VALUES[i];
            }

            public float getSize() {
                return size;
            }
        }
    }

    public enum Theme {
        INSTANCE;

        private static final int LIGHT_THEME_AMBER = R.style.LightTheme_Inverse_Amber;
        public static final int LIGHT_THEME_GREEN = R.style.LightTheme_Inverse_Green;
        public static final int LIGHT_THEME_LIGHT_BLUE = R.style.LightTheme_Inverse_LightBlue;
        private static final int DARK_THEME = R.style.DarkTheme;

        public static final int TRANSLUCENT_THEME_LIGHT = R.style.TranslucentTheme_Light;

        private static final int[] THEMES = {
                LIGHT_THEME_AMBER,
                LIGHT_THEME_GREEN,
                LIGHT_THEME_LIGHT_BLUE,
                DARK_THEME
        };

        private volatile int currentTheme;
        private volatile int currentColorAccent;

        public static boolean isDefaultTheme() {
            // default theme in AndroidManifest.xml is dark theme
            return isDarkTheme();
        }

        public static boolean isDarkTheme() {
            return INSTANCE.currentTheme == DARK_THEME;
        }

        public static int getCurrentTheme() {
            return INSTANCE.currentTheme;
        }

        public static void setCurrentTheme(SharedPreferences sharedPreferences) {
            INSTANCE.currentTheme = THEMES[Integer.parseInt(
                    getSharedPreferencesString(
                            sharedPreferences,
                            MainPreferenceFragment.PREF_KEY_THEME,
                            R.string.pref_theme_default_value))];

            // get current theme's accent color
            TypedArray typedArray = App.getContext()
                    .obtainStyledAttributes(
                            INSTANCE.currentTheme, new int[]{R.attr.colorAccent});
            INSTANCE.currentColorAccent = typedArray.getColor(0, -1);
            typedArray.recycle();

            if (INSTANCE.currentColorAccent == -1) {
                throw new IllegalStateException("Theme accent color can't be -1.");
            }
        }

        @ColorRes
        public static int getCurrentColorAccent() {
            return INSTANCE.currentColorAccent;
        }

        @ColorUtil.Alpha
        private static int getSecondaryTextAlpha() {
            if (INSTANCE.currentTheme == DARK_THEME) {
                return ColorUtil.BLACK_BACKGROUND_SECONDARY_TEXT_ALPHA;
            } else {
                return ColorUtil.WHITE_BACKGROUND_SECONDARY_TEXT_OR_ICONS_ALPHA;
            }
        }

        @ColorUtil.Alpha
        public static int getDisabledOrHintTextAlpha() {
            if (INSTANCE.currentTheme == DARK_THEME) {
                return ColorUtil.BLACK_BACKGROUND_DISABLED_OR_HINT_TEXT_ALPHA;
            } else {
                return ColorUtil.WHITE_BACKGROUND_DISABLED_OR_HINT_TEXT_ALPHA;
            }
        }

        public static int getSecondaryTextColor() {
            return ColorUtil.a(INSTANCE.currentColorAccent, getSecondaryTextAlpha());
        }
    }

    public enum Download {
        INSTANCE;

        private volatile DownloadStrategy avatarsDownloadStrategy;
        private volatile AvatarResolutionStrategy avatarResolutionStrategy;
        private volatile AvatarCacheInvalidationInterval avatarCacheInvalidationInterval;
        private volatile DownloadStrategy imagesDownloadStrategy;

        public static int getTotalCacheSize(SharedPreferences sharedPreferences) {
            String value = getSharedPreferencesString(
                    sharedPreferences,
                    DownloadPreferenceFragment.PREF_KEY_TOTAL_DOWNLOAD_CACHE_SIZE,
                    R.string.pref_download_total_cache_size_default_value);

            return TotalCacheSize.get(Integer.parseInt(value)).size;
        }

        public static void setAvatarsDownloadStrategy(SharedPreferences sharedPreferences) {
            String value = getSharedPreferencesString(
                    sharedPreferences,
                    DownloadPreferenceFragment.PREF_KEY_DOWNLOAD_AVATARS_STRATEGY,
                    R.string.pref_download_avatars_strategy_default_value);

            INSTANCE.avatarsDownloadStrategy = DownloadStrategy.get(Integer.parseInt(value));
        }

        public static boolean needDownloadAvatars() {
            return INSTANCE.avatarsDownloadStrategy.needDownload(General.INSTANCE.hasWifi);
        }

        public static void setAvatarResolutionStrategy(SharedPreferences sharedPreferences) {
            String value = getSharedPreferencesString(
                    sharedPreferences,
                    DownloadPreferenceFragment.PREF_KEY_AVATAR_RESOLUTION_STRATEGY,
                    R.string.pref_avatar_resolution_strategy_default_value);

            INSTANCE.avatarResolutionStrategy = AvatarResolutionStrategy.get(Integer.parseInt(value));
        }

        public static boolean needDownloadHighResolutionAvatars() {
            return INSTANCE.avatarResolutionStrategy.needDownloadHigherResolution(General.INSTANCE.hasWifi);
        }

        public static boolean needMonitorWifi() {
            return INSTANCE.avatarsDownloadStrategy != DownloadStrategy.NOT
                    || INSTANCE.imagesDownloadStrategy != DownloadStrategy.NOT;
        }

        public static void setAvatarCacheInvalidationInterval(SharedPreferences sharedPreferences) {
            String value = getSharedPreferencesString(
                    sharedPreferences,
                    DownloadPreferenceFragment.PREF_KEY_AVATAR_CACHE_INVALIDATION_INTERVAL,
                    R.string.pref_avatar_cache_invalidation_interval_default_value);

            INSTANCE.avatarCacheInvalidationInterval = AvatarCacheInvalidationInterval.get(Integer.parseInt(value));
        }

        public static Key getAvatarCacheInvalidationIntervalSignature() {
            return INSTANCE.avatarCacheInvalidationInterval.getSignature();
        }

        public static void setImagesDownloadStrategy(SharedPreferences sharedPreferences) {
            String value = getSharedPreferencesString(
                    sharedPreferences,
                    DownloadPreferenceFragment.PREF_KEY_DOWNLOAD_IMAGES_STRATEGY,
                    R.string.pref_download_images_strategy_default_value);

            INSTANCE.imagesDownloadStrategy = DownloadStrategy.get(Integer.parseInt(value));
        }

        public static boolean needDownloadImages() {
            return INSTANCE.imagesDownloadStrategy.needDownload(General.INSTANCE.hasWifi);
        }

        private enum TotalCacheSize {
            // 32MB, 64MB, 128MB
            LOW(32), NORMAL(64), HIGH(128);

            private static final TotalCacheSize[] VALUES = TotalCacheSize.values();

            private final int size;

            TotalCacheSize(int size) {
                this.size = size * 1000 * 1000;
            }

            public static TotalCacheSize get(int i) {
                return VALUES[i];
            }
        }

        private enum DownloadStrategy {
            NOT, WIFI, ALWAYS;

            private static final DownloadStrategy[] VALUES = DownloadStrategy.values();

            public static DownloadStrategy get(int i) {
                return VALUES[i];
            }

            public boolean needDownload(boolean hasWifi) {
                return equals(WIFI) && hasWifi
                        || equals(ALWAYS);
            }
        }

        private enum AvatarResolutionStrategy {
            LOW, HIGH_WIFI, HIGH;

            private static final AvatarResolutionStrategy[] VALUES = AvatarResolutionStrategy.values();

            public static AvatarResolutionStrategy get(int i) {
                return VALUES[i];
            }

            public boolean needDownloadHigherResolution(boolean hasWifi) {
                return equals(HIGH_WIFI) && hasWifi
                        || equals(HIGH);
            }
        }

        private enum AvatarCacheInvalidationInterval {
            EVERY_DAY(DateUtil::today),
            EVERY_WEEK(DateUtil::dayOfWeek),
            EVERY_MONTH(DateUtil::dayOfMonth);

            private static final AvatarCacheInvalidationInterval[] VALUES = AvatarCacheInvalidationInterval.values();

            private final Callable<String> callable;

            AvatarCacheInvalidationInterval(Callable<String> callable) {
                this.callable = callable;
            }

            public static AvatarCacheInvalidationInterval get(int i) {
                return VALUES[i];
            }

            public Key getSignature() {
                try {
                    return new StringSignature(callable.call());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to invoke Callable#Call().", e);
                }
            }
        }
    }

    private static String getSharedPreferencesString(SharedPreferences sharedPreferences, String key, @StringRes int defValueResId) {
        return sharedPreferences.getString(key, App.getContext().getString(defValueResId));
    }
}
