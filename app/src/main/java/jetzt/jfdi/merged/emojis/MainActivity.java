package jetzt.jfdi.merged.emojis;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.twitter.sdk.android.core.*;
import com.twitter.sdk.android.core.identity.*;
import com.twitter.sdk.android.core.models.*;
import java.util.*;

import com.twitter.sdk.android.core.Callback;
import retrofit2.*;
import android.text.*;
import android.support.v7.widget.*;
import android.net.*;

public class MainActivity extends Activity {
  private static final int MAX_RETRIES = 10;
  private static final int MAX_RESULTS = 120;
  private static final int LANDSCAPE_COLUMN_COUNT = 12;
  private static final int PORTRAIT_COLUMN_COUNT = 4;
  
  private TwitterLoginButton loginButton = null;
  private Button logOutButton = null;
  private Button searchButton = null;
  private EditText searchEdit = null;
  private RecyclerView recycler = null;
  private View loading = null;

  private EmojiTweetAdapter emojiTweetAdapter = new EmojiTweetAdapter(new EmojiTweetAdapter.EmojiClickedListener() {
      @Override
      public void onEmojiClicked(EmojiTweetAdapter.EmojiTweet tweet) {
        TwitterCore
          .getInstance()
          .getApiClient()
          .getStatusesService()
          .show(
          tweet.tweetId, // tweet id
          true, // trim user
          false, // include my tweet
          false // include entities
        ).enqueue(new Callback<Tweet>() {
            @Override
            public void success(Result<Tweet> result) {
              if (result.data != null) {
                showMoreInfoAboutTweet(result.data);
              }
            }

            @Override
            public void failure(TwitterException exception) {
              error(exception.toString());
            }
          });
      }
    });

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.main);
  }

  @Override
  protected void onStart() {
    super.onStart();
    loginButton = findViewById(R.id.login_button);
    logOutButton = findViewById(R.id.logout_button);
    searchButton = findViewById(R.id.search_button);

    searchEdit = findViewById(R.id.search_edit);

    loading = findViewById(R.id.loading);

    recycler = findViewById(R.id.recycler);
    recycler.setLayoutManager(new GridLayoutManager(this, getColumnCount()));
    recycler.setAdapter(emojiTweetAdapter);

    final TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();
    if (session == null) {
      userIsLoggedOut();
    }
    else {
      userIsLoggedIn();
      showInitial();
    }
  }

  private int getColumnCount() {
    if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
      return LANDSCAPE_COLUMN_COUNT;
    }
    else {
      return PORTRAIT_COLUMN_COUNT;
    }
  }

  private void userIsLoggedIn() {
    loginButton.setVisibility(View.GONE);
    logOutButton.setVisibility(View.VISIBLE);
    recycler.setVisibility(View.VISIBLE);
    searchButton.setVisibility(View.VISIBLE);
    searchEdit.setVisibility(View.VISIBLE);
    loading.setVisibility(View.VISIBLE);

    logOutButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View p1) {
          TwitterCore.getInstance().getSessionManager().clearActiveSession();
          userIsLoggedOut();
        }
      });

    searchButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View p1) {
          showMatching(searchEdit.getText().toString());
        }
      });
  }

  private void userIsLoggedOut() {
    loginButton.setVisibility(View.VISIBLE);
    logOutButton.setVisibility(View.GONE);
    recycler.setVisibility(View.GONE);
    searchButton.setVisibility(View.GONE);
    searchEdit.setVisibility(View.GONE);
    loading.setVisibility(View.GONE);

    loginButton    
      .setCallback(new Callback<TwitterSession>() {
        @Override
        public void success(Result<TwitterSession> result) {
          if (result != null && result.data != null) {
            showInitial();
          }
          else {
            error("Something kaput " + result);
          }
        }

        @Override
        public void failure(TwitterException exception) {
          error(exception.toString());
        }
      });
  }

  private void showInitial() {
    Call<List<Tweet>> callToTweets = TwitterCore
      .getInstance()
      .getApiClient()
      .getStatusesService()
      .userTimeline( 
      null, // id
      "EmojiMashupBot", // screenName
      MAX_RESULTS, // count
      null, // sinceId
      null, // maxId
      true, // trimUser
      true, // excludeReplies
      false, // contributerDetails
      false // includeRetweets
    );

    callToTweets.enqueue(new Callback<List<Tweet>>() {
        @Override
        public void success(Result<List<Tweet>> result) {
          if (result.data != null) {
            emojiTweetAdapter.clearTweets();
            displayTweets(result.data);
          }
        }

        @Override
        public void failure(TwitterException exception) {
          error(exception.toString());
        }
      });
  }

  private void showMatching(final String toMatch) {
    emojiTweetAdapter.clearTweets();
    loading.setVisibility(View.VISIBLE);
    showMatching(toMatch, null, MAX_RETRIES);
  }

  private void showMatching(final String toMatch, final Long maxId, final int maxAttempts) {
    if (maxAttempts < 0) {
      Toast.makeText(this, "No more tweets found!", Toast.LENGTH_LONG).show();
      return;
    }

    Call<List<Tweet>> callToTweets = TwitterCore
      .getInstance()
      .getApiClient()
      .getStatusesService()
      .userTimeline( 
      null, // id
      "EmojiMashupBot", // screenName
      MAX_RESULTS, // count
      null, // sinceId
      maxId, // maxId
      true, // trimUser
      true, // excludeReplies
      false, // contributerDetails
      false // includeRetweets
    );
    callToTweets.enqueue(new Callback<List<Tweet>>() {
        @Override
        public void success(Result<List<Tweet>> result) {
          if (result.data != null && result.data.size() > 0) {
            final List<Tweet> filtered = new ArrayList<>(result.data.size());
            for (final Tweet tweet : result.data) {
              if (tweet.text.contains(toMatch)) {
                filtered.add(tweet);
              }
            }

            displayTweets(filtered);
            if (emojiTweetAdapter.getItemCount() < 120) {
              showMatching(toMatch, result.data.get(result.data.size() - 1).id, maxAttempts - 1);
            }
          }
        }

        @Override
        public void failure(TwitterException exception) {
          error(exception.toString());
        }
      });
  }

  private void displayTweets(List<Tweet> tweets) {
    final List<EmojiTweetAdapter.EmojiTweet> myTweets = new ArrayList<>();

    for (final Tweet tweet: tweets) {
      final String text = extractEmoji(tweet.text);

      final String url;
      if (tweet.entities.media != null && tweet.entities.media.size() == 1) {
        url = tweet.entities.media.get(0).mediaUrlHttps;
        myTweets.add(new EmojiTweetAdapter.EmojiTweet(tweet.id, text, url));
      }      
    }

    loading.setVisibility(View.GONE);
    emojiTweetAdapter.addTweets(myTweets);
  }

  private String extractEmoji(String tweet) {
    final StringBuilder builder = new StringBuilder();
    final String[] emojiContainingTexts = tweet.split(" = ")[0].split(" \\+ ");

    for (final String emoji : emojiContainingTexts) {
      final int codepoint = emoji.trim().codePointAt(0);
      builder.append(Character.toChars(codepoint));
    }

    return builder.toString();
  }

  private void info(CharSequence message) {
    new AlertDialog.Builder(this)
      .setTitle("Info")
      .setMessage(message)
      .setPositiveButton(
      android.R.string.ok,
      new AlertDialog.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int button) {
          dialog.dismiss();
        }
      }
    ).show();
  }

  private void error(String message) {
    new AlertDialog.Builder(this)
      .setTitle("Error")
      .setMessage(message)
      .setPositiveButton(
      android.R.string.ok,
      new AlertDialog.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int button) {
          dialog.dismiss();
        }
      }
    ).show();
  }

  private void showMoreInfoAboutTweet(final Tweet tweet) {
    new AlertDialog.Builder(this)
      .setTitle("Tweet Information")
      .setMessage(tweet.text)
      .setNeutralButton(
      "Open Tweet",
      new AlertDialog.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int button) {
          dialog.dismiss();

          final String link = "https://mobile.twitter.com/EmojiMashupBot/status/" + tweet.id;
          final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
          startActivity(browserIntent);
        }
      }
    ).setNegativeButton(
      "Copy Image Url",
      new AlertDialog.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int button) {
          dialog.dismiss();

          if (tweet.extendedEntities.media.size() == 0) {
            Toast.makeText(MainActivity.this, "No image found to copy.", Toast.LENGTH_LONG).show();
          } else {
            final String link = tweet.extendedEntities.media.get(0).mediaUrlHttps;
            final android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
            final ClipData clip = ClipData.newPlainText("Merged Emoji image link", link);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(MainActivity.this, "Tweet Image url copied to clipboard", Toast.LENGTH_LONG).show();
          }          
        }
      }
    ).setPositiveButton(
      android.R.string.ok,
      new AlertDialog.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int button) {
          dialog.dismiss();
        }
      }
    ).show();
  }
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    loginButton.onActivityResult(requestCode, resultCode, data);
  }
}
