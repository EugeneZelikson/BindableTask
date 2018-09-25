package seven.of.hearts.bindabletask.task;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.View;

import seven.of.hearts.R;
import seven.of.hearts.bindabletask.MainActivity;
import seven.of.hearts.base.BaseFragment;
import seven.of.hearts.di.viewmodel.Injectable;
import seven.of.hearts.databinding.FragmentTaskBinding;
import seven.of.hearts.utils.KeyboardStateManager;
import seven.of.hearts.view.ButtonDayPicker;
import seven.of.hearts.view.DialogProjectsList;
import seven.of.hearts.bindabletask.task.TaskViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import javax.inject.Inject;

/*
 * Created by Eugene Zelikson (7ofHearts).
 */

public class TaskFragment extends BaseFragment<FragmentTaskBinding> implements
        ButtonDayPicker.PickerListener,
        ProjectsPickerAdapter.ItemClickListener, Injectable {

    private static final String TAG = TaskFragment.class.getSimpleName();
    private static final String PROJECT_ID_KEY = "ID_PROJECT";
    private static final String TASK_ID_KEY = "ID_TASK";

    @Inject
    protected TaskViewModel taskViewModel;

    @Inject
    protected ProjectsPickerAdapter adapter;

    private DialogProjectsList dialog;

//    BaseFragment methods.

    @Override
    protected int getLayout() {
        return R.layout.fragment_task;
    }

//    Lifecycle methods.

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUiSettings();
        setViewModelSettings();
    }

//    Initialize UI.

    private void setUiSettings() {
        binding.checkbox.setTypeface(ResourcesCompat.getFont(Objects.requireNonNull(getContext()),
                R.font.m_medium));
        updateActivityUi();
        binding.setModel(taskViewModel);
        binding.dayPicker.setListener(this);
    }

    // Updating title on Activity.
    private void updateActivityUi() {
        if (getActivity() != null) {
            ((MainActivity) getActivity()).setTitleOnToolbar(
                    getString(getIdByKey(TASK_ID_KEY) != -1
                            ? R.string.task_refactoring
                            : R.string.new_task));
        }
    }

//    Initialize View Model.

    private void setViewModelSettings() {
        taskViewModel.loadInitData();
        setDataObservers();
        setEventObservers();
        checkOnProjectArguments();
        checkOnTaskArguments();
    }

//    Checking start data for task.

    private void checkOnProjectArguments() {
        try {
            int projectId = getIdByKey(PROJECT_ID_KEY);
            if (projectId != -1) {
                taskViewModel.getTask().getValue().projectID = projectId;
            }
            binding.project.setText(taskViewModel.getProject(taskViewModel.getTask().getValue().projectID).name);
            binding.project.setEnabled(false);
        } catch (Exception exception) {
            Log.d(TAG, "checkOnProjectArguments: " + exception.getMessage());
        }
    }

    private void checkOnTaskArguments() {
        try {
            int taskId = getIdByKey(TASK_ID_KEY);
            if (taskId != -1) {
                taskViewModel.loadTaskByID(taskId);
                binding.project.setText(taskViewModel.getProject(taskViewModel.getTask().getValue().projectID).name);
            }
        } catch (Exception exception) {
            Log.d(TAG, "onViewCreated: " + exception.getMessage());
        }
    }

    private int getIdByKey(String key) {
        return getArguments() != null ? getArguments().getInt(key, -1) : -1;
    }

//    Setting observers to view model.

    private void setDataObservers() {
        taskViewModel.getProjects().observe(this, projects -> adapter.setItems(projects));
        taskViewModel.getRepeatType().observe(this, type -> {
            if(type != null) {
                switch (type) {
                    case 0:
                        everyDaySelected();
                        break;
                    case 1:
                        everyWeekSelected();
                        break;
                    case 2:
                        exactlyDateSelected();
                        break;
                }
            }
        });
    }

    private void setEventObservers() {
        taskViewModel.getProjectsDialogEvent().observe(this, mVoid -> openProjectsListDialog());
        taskViewModel.getEndDateCalendarEvent().observe(this, mVoid -> openEstimateDateCalendar());
        taskViewModel.getRepeatDateCalendarEvent().observe(this, mVoid -> openExactlyDateCalendar());
        taskViewModel.getRegularTaskEvent().observe(this, isRegular -> {
            if(isRegular != null) {
                binding.everyWeekConstraint.setVisibility(isRegular ? View.VISIBLE : View.GONE);
            }
        });
        taskViewModel.getDayPickerErrorEvent().observe(this, mVoid -> showErrorDialog(getString(R.string.choose_every_week_repeat_days)));
        taskViewModel.getNameFieldErrorEvent().observe(this, mVoid -> {
            binding.tilTask.setErrorEnabled(true);
            binding.tilTask.setError(getString(R.string.enter_task_name));
            binding.getRoot().clearFocus();
        });
        taskViewModel.getEmptyProjectErrorEvent().observe(this, aVoid -> {
            binding.tilProject.setErrorEnabled(true);
            binding.tilProject.setError(getString(R.string.choose_project));
            binding.getRoot().clearFocus();
        });
        taskViewModel.getRepeatDateErrorEvent().observe(this, mVoid -> showErrorDialog(getString(R.string.exactly_repeat_date_empty_field)));
        taskViewModel.getEveryWeekEvent().observe(this, mVoid -> binding.dayPicker.setSelectedDays(taskViewModel.getTask().getValue().pickedDays));
        taskViewModel.getSaveEvent().observe(this, mVoid -> {
            if (getActivity() != null) {
                KeyboardStateManager.hideSoftKeyboard(getActivity());
                getActivity().onBackPressed();
            }
        });
    }

//    Regular task UI.

    private void everyDaySelected() {
        binding.dayPicker.setVisibility(View.GONE);
        binding.exactlyDateLabel.setVisibility(View.GONE);
        binding.exactlyDate.setVisibility(View.GONE);
    }

    private void everyWeekSelected() {
        binding.dayPicker.setVisibility(View.VISIBLE);
        binding.exactlyDateLabel.setVisibility(View.GONE);
        binding.exactlyDate.setVisibility(View.GONE);
    }

    private void exactlyDateSelected() {
        binding.dayPicker.setVisibility(View.GONE);
        binding.exactlyDateLabel.setVisibility(View.VISIBLE);
        binding.exactlyDate.setVisibility(View.VISIBLE);
    }

//    Dialogs.

    private void openEstimateDateCalendar() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                R.style.DialogTheme, (datePicker, year, monthOfYear, dayOfMonth) -> {
            String pickedDate = dayOfMonth + "." + (monthOfYear + 1) + "." + year;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
            try {
                Date date = simpleDateFormat.parse(pickedDate);
                if (date.after(new Date())) {
                    date.setTime(date.getTime());
                    binding.date.setText(simpleDateFormat.format(date));
                } else {
                    showErrorDialog(getString(R.string.wrong_pickеd_date));
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
        datePickerDialog.show();
    }

    private void openExactlyDateCalendar() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                R.style.DialogTheme, (datePicker, year, monthOfYear, dayOfMonth) -> {
            String pickedDate = dayOfMonth + "." + (monthOfYear + 1) + "." + year;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
            Log.d(TAG, "openExactlyDateCalendar: 123");
            try {
                Date date = simpleDateFormat.parse(pickedDate);
                if (date.after(new Date())) {
                    date.setTime(date.getTime());
                    binding.exactlyDate.setText(simpleDateFormat.format(date));
                } else {
                    showErrorDialog(getString(R.string.wrong_pickеd_date));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
        datePickerDialog.show();
    }

    private void openProjectsListDialog() {
        dialog = new DialogProjectsList(getContext());
        dialog.setAdapter(adapter);
        dialog.show();
    }

    private void showErrorDialog(String message) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
        alertDialog.setPositiveButton(getString(R.string.ok), (dialogInterface, i) -> dialogInterface.dismiss());
        alertDialog.setTitle(getString(R.string.error));
        alertDialog.setMessage(message);
        alertDialog.show();
    }

//    Event listeners.

    @Override
    public void onPickerButtonClick(String pickedDays) {
        taskViewModel.setPickedDays(pickedDays);
    }

    @Override
    public void OnItemClick(CharSequence projectName, int projectId) {
        taskViewModel.getTask().getValue().projectID = projectId;
        binding.project.setText(projectName);
        dialog.dismiss();
    }
}

