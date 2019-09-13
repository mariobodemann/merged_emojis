package jetzt.jfdi.merged.emojis;
import android.app.*;
import com.twitter.sdk.android.core.*;

public class EmojiApplication extends Application {

  @Override
  public void onCreate() {
    Twitter.initialize(this);
    super.onCreate();
  }
}
