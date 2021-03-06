package au.com.codeka.warworlds.client.game.solarsystem;


import static com.google.common.base.Preconditions.checkNotNull;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.store.StarCursor;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Star;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.Locale;

class StarSearchListAdapter extends BaseAdapter {
  private LayoutInflater inflater;
  private StarCursor cursor;

  // We keep the last 5 stars you've visited in an LRU cache so we can display them at the top
  // of the search list (note we actually keep 6 but ignore the most recent one, which is always
  // "this star").
  private static final ArrayList<Star> lastStars = new ArrayList<>();
  private static final int LAST_STARS_MAX_SIZE = 6;

  private static final int VIEW_TYPE_STAR = 0;
  private static final int VIEW_TYPE_SEPARATOR = 1;
  private static final int NUM_VIEW_TYPES = 2;

  StarSearchListAdapter(LayoutInflater inflater) {
    this.inflater = inflater;
  }

  /** Sets the {@link StarCursor} that we'll use to display stars. */
  void setCursor(StarCursor cursor) {
    this.cursor = checkNotNull(cursor);
    notifyDataSetChanged();
  }

  void addToLastStars(Star star) {
    synchronized (lastStars) {
      for (int i = 0; i < lastStars.size(); i++) {
        if (lastStars.get(i).id.equals(star.id)) {
          lastStars.remove(i);
          break;
        }
      }
      lastStars.add(0, star);
      while (lastStars.size() > LAST_STARS_MAX_SIZE) {
        lastStars.remove(lastStars.size() - 1);
      }
    }
    notifyDataSetChanged();
  }


  @Override
  public int getViewTypeCount() {
    return NUM_VIEW_TYPES;
  }

  @Override
  public int getItemViewType(int position) {
    if (position == lastStars.size() - 1) {
      return VIEW_TYPE_SEPARATOR;
    } else {
      return VIEW_TYPE_STAR;
    }
  }

  @Override
  public int getCount() {
    int count = lastStars.size() - 1;
    if (cursor != null) {
      count += cursor.getSize() + 1; // +1 for the spacer view
    }
    return count;
  }

  @Override
  public Object getItem(int position) {
    if (position < lastStars.size() - 1) {
      return lastStars.get(position + 1);
    } else if (position == lastStars.size() - 1) {
      return null;
    } else {
      return cursor.getValue(position - lastStars.size());
    }
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View view = convertView;
    if (view == null) {
      if (position == lastStars.size() - 1) {
        // it's just a spacer
        view = new View(inflater.getContext());
        view.setLayoutParams(
            new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 20));
      } else {
        view = inflater.inflate(R.layout.solarsystem_starlist_row, parent, false);
      }
    }

    if (position == lastStars.size() - 1) {
      return view;
    }

    Star star = (Star) getItem(position);
    ImageView starIcon = view.findViewById(R.id.star_icon);
    TextView starName = view.findViewById(R.id.star_name);
    TextView starType = view.findViewById(R.id.star_type);
    TextView starGoodsDelta = view.findViewById(R.id.star_goods_delta);
    TextView starGoodsTotal = view.findViewById(R.id.star_goods_total);
    TextView starMineralsDelta = view.findViewById(R.id.star_minerals_delta);
    TextView starMineralsTotal = view.findViewById(R.id.star_minerals_total);

    if (starIcon == null) {
      throw new RuntimeException(Integer.toString(position) + " " + view.toString());
    }

    if (star == null) {
      starIcon.setImageBitmap(null);
      starName.setText("");
      starType.setText("");
      starGoodsDelta.setText("");
      starGoodsTotal.setText("???");
      starMineralsDelta.setText("");
      starMineralsTotal.setText("???");
    } else {
      Picasso.with(view.getContext())
          .load(ImageHelper.getStarImageUrl(inflater.getContext(), star, 36, 36))
          .into(starIcon);
      starName.setText(star.name);
      starType.setText(star.classification.toString());

      Empire myEmpire = EmpireManager.i.getMyEmpire();
      EmpireStorage storage = null;
      for (int i = 0; i < star.empire_stores.size(); i++) {
        if (star.empire_stores.get(i).empire_id != null
            && star.empire_stores.get(i).empire_id.equals(myEmpire.id)) {
          storage = star.empire_stores.get(i);
          break;
        }
      }

      if (storage == null) {
        starGoodsDelta.setText("");
        starGoodsTotal.setText("");
        starMineralsDelta.setText("");
        starMineralsTotal.setText("");
      } else {
        starGoodsDelta.setText(String.format(Locale.ENGLISH, "%s%d/hr",
            storage.goods_delta_per_hour < 0 ? "-" : "+",
            Math.abs(Math.round(storage.goods_delta_per_hour))));
        if (storage.goods_delta_per_hour < 0) {
          starGoodsDelta.setTextColor(Color.RED);
        } else {
          starGoodsDelta.setTextColor(Color.GREEN);
        }
        starGoodsTotal.setText(String.format(Locale.ENGLISH, "%d / %d",
            Math.round(storage.total_goods),
            Math.round(storage.max_goods)));

        starMineralsDelta.setText(String.format(Locale.ENGLISH, "%s%d/hr",
            storage.minerals_delta_per_hour < 0 ? "-" : "+",
            Math.abs(Math.round(storage.minerals_delta_per_hour))));
        if (storage.minerals_delta_per_hour < 0) {
          starMineralsDelta.setTextColor(Color.RED);
        } else {
          starMineralsDelta.setTextColor(Color.GREEN);
        }
        starMineralsTotal.setText(String.format(Locale.ENGLISH, "%d / %d",
            Math.round(storage.total_minerals),
            Math.round(storage.max_minerals)));
      }
    }
    return view;
  }
}