package jetzt.jfdi.merged.emojis;
import android.support.v7.widget.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import com.bumptech.glide.*;
import android.view.View.*;

public class EmojiTweetAdapter extends RecyclerView.Adapter<EmojiTweetAdapter.EmojiViewHolder> {
  public static interface EmojiClickedListener {
    public void onEmojiClicked(EmojiTweet tweet);
  }  

  public static class EmojiViewHolder extends RecyclerView.ViewHolder {

    public ViewGroup content;
    public ImageView image;
    public TextView text;

    public EmojiViewHolder(View view) {
      super(view);

      content = view.findViewById(R.id.card_content);
      image = view.findViewById(R.id.card_image);
      text = view.findViewById(R.id.card_text);
    }
  }

  public static class EmojiTweet {
    public final Long tweetId;
    public final String text;
    public final String imageUrl;

    public EmojiTweet(Long tweetId, String text, String imageUrl) {
      this.tweetId = tweetId;
      this.text = text;
      this.imageUrl = imageUrl;
    }
  }

  private List<EmojiTweet> tweets = new ArrayList<>();
  private EmojiClickedListener listener = null;
  private RequestManager glide = null;

  public EmojiTweetAdapter(EmojiClickedListener listener) {
    this.listener = listener;
  }

  public void addTweets(List<EmojiTweet> newones) {
    tweets.addAll(newones);
    notifyDataSetChanged();
  }

  public void clearTweets() {
    tweets.clear();
    notifyDataSetChanged();
  }

  @Override
  public EmojiTweetAdapter.EmojiViewHolder onCreateViewHolder(ViewGroup parent, int position) {
    final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    final View view = inflater.inflate(R.layout.item, parent, false);

    return new EmojiViewHolder(view);
  }

  @Override
  public void onBindViewHolder(EmojiTweetAdapter.EmojiViewHolder vh, int position) {
    final EmojiTweet tweet = tweets.get(position);
    vh.text.setText(tweet.text);

    vh.content.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View view) {
          if (listener != null) {
            listener.onEmojiClicked(tweet);
          }
        }  
      }
    );

    if (glide == null) {
      glide =  Glide.with(vh.content.getContext());
    }    

    glide.load(tweet.imageUrl).into(vh.image);  
  }

  @Override
  public int getItemCount() {
    return tweets.size();
  }
}
