package de.zitzmanncedric.abicalc.fragments.subject;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import de.zitzmanncedric.abicalc.AppCore;
import de.zitzmanncedric.abicalc.R;
import de.zitzmanncedric.abicalc.activities.subject.GradeEditorActivity;
import de.zitzmanncedric.abicalc.adapter.AdvancedSubjectListAdapter;
import de.zitzmanncedric.abicalc.api.Grade;
import de.zitzmanncedric.abicalc.api.Subject;
import de.zitzmanncedric.abicalc.api.list.ListableObject;
import de.zitzmanncedric.abicalc.database.AppDatabase;
import de.zitzmanncedric.abicalc.listener.OnListItemCallback;
import de.zitzmanncedric.abicalc.utils.AppSerializer;
import lombok.Getter;
import needle.Needle;
import needle.UiRelatedProgressTask;

/**
 * Zeigt die Übersicht über alle Noten eines Halbjahres an
 * @author Cedric Zitzmann
 */
public class GradesFragment extends Fragment implements OnListItemCallback {

    private Subject subject;
    @Getter private int termID;

    private AdvancedSubjectListAdapter adapter;

    private TextView noticeView;
    private LinearLayout wrapper;

    /**
     * Konstruktor der Klasse. (Durch Veerbung benötigt) (leer)
     */
    public GradesFragment() { }

    /**
     * Konstuktor der Klasse. Legt das Fach und die Halbjahres-ID fest
     * @param subject Fach
     * @param termID Halbjahres-ID
     */
    public GradesFragment(Subject subject, int termID) {
        this.subject = subject;
        this.termID = termID;
    }

    /**
     * Das Layout wird bestimmt. Werden keine Noten gefunden wird eine Nachricht eingeblendet
     * @param inflater Inflater zum erstellen des Layouts
     * @param container Der View, der das Layout umschließt
     * @param savedInstanceState Von Android übergeben (nicht genutzt)
     * @return Erstelltes View-Element aus dem Layout
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_grades, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.grades_list);
        adapter = new AdvancedSubjectListAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        recyclerView.setAdapter(adapter);

        wrapper = view.findViewById(R.id.fragment_wrapper);
        ArrayList<ListableObject> grades = new ArrayList<>(AppDatabase.getInstance().getGradesForTerm(subject, termID));

        noticeView = new TextView(new ContextThemeWrapper(view.getContext(), R.style.TextAppearance));
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noticeView.setLayoutParams(params);
        noticeView.setAlpha(0.6f);
        noticeView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        noticeView.setText(view.getContext().getString(R.string.label_grades_missing));

        if(grades.isEmpty()) {
            wrapper.addView(noticeView);
        } else {
            populate();
        }
        return view;
    }

    /**
     * Private Funktion. Sorgt dafür, dass die Noten geladen werden und in der Liste angzeigt werden
     */
    private void populate(){
        adapter.clear();

        Needle.onBackgroundThread().execute(new UiRelatedProgressTask<Void, ListableObject>() {
            @Override
            protected Void doWork() {
                ArrayList<ListableObject> grades = new ArrayList<>(AppDatabase.getInstance().getGradesForTerm(subject, termID));

                for (ListableObject grade : grades) {
                    publishProgress(grade);
                }
                try {
                    Thread.sleep(100);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(ListableObject grade) {
                adapter.add(grade);
            }

            @Override
            protected void thenDoUiRelatedWork(Void aVoid) {
                if(adapter.getItemCount() > 0) {
                    if(noticeView != null) wrapper.removeView(noticeView);
                }
            }
        });
    }

    /**
     * Wird auf eine Note geklickt öffnet sich der Noteneditor. Dort kann die Note nun bearbeitet werden
     * @param object Angeklicktes Element
     */
    @Override
    public void onItemClicked(ListableObject object) {
        Intent intent = new Intent(getContext(), GradeEditorActivity.class);

        if(object instanceof Grade) {
            Grade grade = (Grade) object;
            intent.putExtra("action", "edit");
            intent.putExtra("grade", AppSerializer.serialize(grade));
            startActivityForResult(intent, AppCore.RequestCodes.REQUEST_UPDATE_GRADE);
        }
    }

    /**
     * Wird auf den "Löschen"-Button des Listenelements geklickt, wird die Note gelöscht
     * @param object Angeklicktes Element
     */
    @Override
    public void onItemDeleted(ListableObject object) {
        if(object instanceof Grade) {
            AppDatabase.getInstance().removeGrade((Grade) object);
            adapter.remove(object);

            if(adapter.getItemCount() == 0) {
                wrapper.addView(noticeView);
            }
        }
    }

    /**
     * Unwichtig (nicht genutzt)
     */
    @Override
    public void onItemEdit(ListableObject object) { }

    /**
     * Unwichtig (nicht genutzt)
     */
    @Override
    public void onItemLongClicked(ListableObject object) { }

    /**
     * Von Android implementiert. Fängt das Resultat durch eine geschlossene Aktivität ab. Bei Erfolg wird das ausgewählte Element in der Liste aktualisiert oder hinzugefügt
     * @param requestCode Code, zur Identifizierung der Anfrage
     * @param resultCode Code, zur Identifizierung des Resultats
     * @param data Zurückgegebene Daten
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == AppCore.RequestCodes.REQUEST_ADD_GRADE) {
            new Handler().postDelayed(() -> {
                try {
                    if (data != null) {
                        byte[] bytes = data.getByteArrayExtra("grade");

                        Grade grade = (Grade) AppSerializer.deserialize(bytes);
                        adapter.add(grade);

                        if (adapter.getItemCount() > 0) {
                            if (noticeView != null) wrapper.removeView(noticeView);
                        }
                    }
                } catch (NullPointerException ex) {
                    ex.printStackTrace();
                }
            }, 100);
        }

        if(requestCode == AppCore.RequestCodes.REQUEST_UPDATE_GRADE) {
            new Handler().postDelayed(() -> {
                try {
                    if (data != null) {
                        Grade oldGrade = (Grade) AppSerializer.deserialize(data.getByteArrayExtra("oldGrade"));
                        Grade newGrade = (Grade) AppSerializer.deserialize(data.getByteArrayExtra("newGrade"));

                        adapter.update(oldGrade, newGrade);

                        if (adapter.getItemCount() > 0) {
                            if (noticeView != null) wrapper.removeView(noticeView);
                        }
                    }
                } catch (NullPointerException ex) {
                    ex.printStackTrace();
                }
            }, 100);
        }

    }
}
