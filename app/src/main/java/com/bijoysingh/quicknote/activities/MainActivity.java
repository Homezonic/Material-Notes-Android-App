package com.bijoysingh.quicknote.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bijoysingh.quicknote.R;
import com.bijoysingh.quicknote.activities.sheets.HomeNavigationBottomSheet;
import com.bijoysingh.quicknote.activities.sheets.SettingsOptionsBottomSheet;
import com.bijoysingh.quicknote.database.Note;
import com.bijoysingh.quicknote.items.EmptyRecyclerItem;
import com.bijoysingh.quicknote.items.NoteRecyclerItem;
import com.bijoysingh.quicknote.items.RecyclerItem;
import com.bijoysingh.quicknote.recyclerview.NoteAppAdapter;
import com.bijoysingh.quicknote.utils.NoteState;
import com.github.bijoysingh.starter.async.MultiAsyncTask;
import com.github.bijoysingh.starter.async.SimpleThreadExecutor;
import com.github.bijoysingh.starter.prefs.DataStore;
import com.github.bijoysingh.starter.recyclerview.RecyclerViewBuilder;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;

public class MainActivity extends ThemedActivity {

  RecyclerView recyclerView;
  NoteAppAdapter adapter;
  NoteState mode;
  DataStore store;

  ImageView addList, homeNav, addRichNote, homeOptions, backButton, searchIcon, searchBackButton, searchCloseIcon;
  TextView addNote;
  EditText searchBox;
  View mainToolbar, searchToolbar, bottomToolbar;

  boolean isInSearchMode;
  List<Note> searchNotes;
  SimpleThreadExecutor executor;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    adapter = new NoteAppAdapter(this);
    mode = NoteState.DEFAULT;
    store = DataStore.get(this);
    executor = new SimpleThreadExecutor(1);

    setupRecyclerView();
    setListeners();
    requestSetNightMode(store.get(ThemedActivity.Companion.getKey(), false));
  }

  public void setListeners() {
    mainToolbar = findViewById(R.id.main_toolbar);
    searchToolbar = findViewById(R.id.search_toolbar);
    bottomToolbar = findViewById(R.id.bottom_toolbar_layout);

    addNote = findViewById(R.id.menu_add_note);
    addNote.setOnClickListener(openNewNoteActivity());

    searchIcon = findViewById(R.id.home_search_button);
    searchIcon.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        setSearchMode(true);
        searchBox.requestFocus();
      }
    });

    searchBackButton = findViewById(R.id.search_back_button);
    searchBackButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onBackPressed();
      }
    });

    searchCloseIcon = findViewById(R.id.search_close_button);
    searchCloseIcon.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        searchBox.setText("");
      }
    });

    searchBox = findViewById(R.id.search_box);
    searchBox.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override
      public void onTextChanged(final CharSequence charSequence, int i, int i1, int i2) {
        executor.executeNow(new Runnable() {
          @Override
          public void run() {
            final List<RecyclerItem> items = search(charSequence.toString());
            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                adapter.setItems(items);
              }
            });
          }
        });
      }

      @Override
      public void afterTextChanged(Editable editable) {

      }
    });

    addList = findViewById(R.id.menu_add_list);
    addList.setOnClickListener(openNewListNoteActivity());

    homeNav = findViewById(R.id.menu_home_nav);
    homeNav.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        HomeNavigationBottomSheet.Companion.openSheet(MainActivity.this);
      }
    });

    addRichNote = findViewById(R.id.menu_add_rich_note);
    addRichNote.setOnClickListener(openNewRichNoteActivity());

    homeOptions = findViewById(R.id.home_option_button);
    homeOptions.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        SettingsOptionsBottomSheet.Companion.openSheet(MainActivity.this);
      }
    });

    backButton = findViewById(R.id.back_button);
    backButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onBackPressed();
      }
    });
  }

  public void setupRecyclerView() {
    recyclerView = new RecyclerViewBuilder(this)
        .setView(this, R.id.recycler_view)
        .setAdapter(adapter)
        .build();
  }

  private void loadNoteByStates(final String[] states) {
    MultiAsyncTask.execute(this, new MultiAsyncTask.Task<List<Note>>() {
      @Override
      public List<Note> run() {
        return Note.db(MainActivity.this).getByNoteState(states);
      }

      @Override
      public void handle(List<Note> notes) {
        adapter.clearItems();

        if (notes.isEmpty()) {
          adapter.addItem(new EmptyRecyclerItem());
        }

        for (Note note : notes) {
          adapter.addItem(new NoteRecyclerItem(note));
        }
      }
    });
  }

  public NoteState getMode() {
    return mode == null ? NoteState.DEFAULT : mode;
  }

  public void onHomeClick() {
    mode = NoteState.DEFAULT;
    loadNoteByStates(new String[]{NoteState.DEFAULT.name(), NoteState.FAVOURITE.name()});
  }

  public void onFavouritesClick() {
    mode = NoteState.FAVOURITE;
    loadNoteByStates(new String[]{NoteState.FAVOURITE.name()});
  }

  public void onArchivedClick() {
    mode = NoteState.ARCHIVED;
    loadNoteByStates(new String[]{NoteState.ARCHIVED.name()});
  }

  public void onTrashClick() {
    mode = NoteState.TRASH;
    loadNoteByStates(new String[]{NoteState.TRASH.name()});
  }

  public View.OnClickListener openNewNoteActivity() {
    return new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent(getApplicationContext(), CreateSimpleNoteActivity.class);
        intent.putExtra(ThemedActivity.Companion.getKey(), isNightMode());
        startActivity(intent);
      }
    };
  }

  public View.OnClickListener openNewRichNoteActivity() {
    return new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent(getApplicationContext(), CreateOrEditAdvancedNoteActivity.class);
        intent.putExtra(ThemedActivity.Companion.getKey(), isNightMode());
        startActivity(intent);
      }
    };
  }

  public View.OnClickListener openNewListNoteActivity() {
    return new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent(getApplicationContext(), CreateAdvancedListActivity.class);
        intent.putExtra(ThemedActivity.Companion.getKey(), isNightMode());
        startActivity(intent);
      }
    };
  }

  public void moveItemToTrashOrDelete(Note note) {
    if (mode == NoteState.TRASH) {
      note.delete(this);
      setupData();
      return;
    }
    markItem(note, NoteState.TRASH);
  }

  public void updateNote(Note note) {
    note.save(this);
    setupData();
  }

  public void markItem(Note note, NoteState state) {
    note.mark(this, state);
    setupData();
  }

  @Override
  protected void onResume() {
    super.onResume();
    setupData();
  }

  private void setupData() {
    mode = getMode();
    switch (mode) {
      case FAVOURITE:
        onFavouritesClick();
        return;
      case ARCHIVED:
        onArchivedClick();
        return;
      case TRASH:
        onTrashClick();
        return;
      default:
      case DEFAULT:
        onHomeClick();
    }
  }

  private void setSearchMode(boolean mode) {
    isInSearchMode = mode;
    mainToolbar.setVisibility(isInSearchMode ? View.GONE : View.VISIBLE);
    bottomToolbar.setVisibility(isInSearchMode ? View.GONE : View.VISIBLE);
    searchToolbar.setVisibility(isInSearchMode ? View.VISIBLE : View.GONE);
    searchBox.setText("");

    if (isInSearchMode) {
      tryOpeningTheKeyboard();
      searchNotes = new ArrayList<>();
      for (RecyclerItem item : adapter.getItems()) {
        if (item instanceof NoteRecyclerItem) {
          searchNotes.add(((NoteRecyclerItem) item).note);
        }
      }
    } else {
      searchNotes = null;
      setupData();
    }
  }

  private List<RecyclerItem> search(String keyword) {
    if (searchNotes == null) {
      return adapter.getItems();
    }

    List<RecyclerItem> notes = new ArrayList<>();
    for (Note note : searchNotes) {
      if (note.search(keyword)) {
        notes.add(new NoteRecyclerItem(note));
      }
    }
    return notes;
  }

  private void tryOpeningTheKeyboard() {
    try {
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    } catch (Exception exception) {
      // Do nothing
    }
  }

  @Override
  public void onBackPressed() {
    if (isInSearchMode) {
      if (searchBox.getText().toString().isEmpty()) {
        setSearchMode(false);
      } else {
        searchBox.setText("");
      }
    } else {
      super.onBackPressed();
    }
  }

  @Override
  public void notifyNightModeChange() {
    store.put(ThemedActivity.Companion.getKey(), isNightMode());
    setSystemTheme();

    View containerLayout = findViewById(R.id.container_layout);
    containerLayout.setBackgroundColor(getThemeColor());

    int toolbarIconColor = getColor(R.color.material_blue_grey_700, R.color.light_secondary_text);
    addList.setColorFilter(toolbarIconColor);
    homeNav.setColorFilter(toolbarIconColor);
    addRichNote.setColorFilter(toolbarIconColor);
    homeOptions.setColorFilter(toolbarIconColor);
    addNote.setTextColor(toolbarIconColor);
    searchIcon.setColorFilter(toolbarIconColor);
    searchBackButton.setColorFilter(toolbarIconColor);
    searchCloseIcon.setColorFilter(toolbarIconColor);

    findViewById(R.id.separator).setVisibility(isNightMode() ? GONE : View.VISIBLE);

    TextView actionBarTitle = findViewById(R.id.action_bar_title);
    actionBarTitle.setTextColor(getColor(R.color.dark_tertiary_text, R.color.light_secondary_text));
    backButton.setColorFilter(getColor(R.color.colorAccent, R.color.material_pink_300));

    int textColor = getColor(R.color.dark_secondary_text, R.color.light_secondary_text);
    int textHintColor = getColor(R.color.dark_hint_text, R.color.light_hint_text);
    searchBox.setTextColor(textColor);
    searchBox.setHintTextColor(textHintColor);

    bottomToolbar.setBackgroundColor(
        getColor(R.color.material_grey_50, R.color.material_grey_850));
  }

}
