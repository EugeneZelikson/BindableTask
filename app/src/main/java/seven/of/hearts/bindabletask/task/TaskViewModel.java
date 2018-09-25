package seven.of.hearts.bindabletask.task;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import seven.of.hearts.base.event.SingleLiveEvent;
import seven.of.hearts.data.database.Project;
import seven.of.hearts.data.database.task.Task;
import seven.of.hearts.data.repository.Repository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

/*
 * Created by Eugene Zelikson (7ofHearts).
 */

public class TaskViewModel extends ViewModel {

    @Inject
    protected Repository repository;

    private SingleLiveEvent<Void> projectDialogEvent = new SingleLiveEvent<>();
    private SingleLiveEvent<Void> endDateCalendarEvent = new SingleLiveEvent<>();
    private SingleLiveEvent<Void> repeatDateCalendarEvent = new SingleLiveEvent<>();
    private SingleLiveEvent<Boolean> regularTaskEvent = new SingleLiveEvent<>();
    private SingleLiveEvent<Void> dayPickerErrorEvent = new SingleLiveEvent<>();
    private SingleLiveEvent<Void> nameFieldErrorEvent = new SingleLiveEvent<>();
    private SingleLiveEvent<Void> emptyProjectErrorEvent = new SingleLiveEvent<>();
    private SingleLiveEvent<Void> saveEvent = new SingleLiveEvent<>();
    private SingleLiveEvent<Void> repeatDateErrorEvent = new SingleLiveEvent<>();
    private SingleLiveEvent<Void> everyWeekEvent = new SingleLiveEvent<>();

    private MutableLiveData<Task> task = new MutableLiveData<>();
    private MutableLiveData<Integer> repeatType = new MutableLiveData<>();
    private MutableLiveData<List<Project>> projects = new MutableLiveData<>();
    private MutableLiveData<Boolean> isTaskBeingEdited = new MutableLiveData<>();

    private boolean isThisTaskUpdating;

    @Inject
    public TaskViewModel() {
        task.setValue(new Task());
        regularTaskEvent.setValue(task.getValue().isRegularTask);
        repeatType.setValue(task.getValue().repeatValue);
        isTaskBeingEdited.setValue(false);
    }

//    Initialization

    public void loadInitData() {
        loadProjects();
    }

//    Updating value in task.

    public void setPickedDays(String pickedDays) {
        task.getValue().pickedDays = pickedDays;
    }

//    Get data from repository.

    public MutableLiveData<Task> getTask() {
        return task;
    }

    public LiveData<List<Project>> getProjects() {
        return projects;
    }

    private void loadProjects() {
        new Thread(() -> {
            List<Project> projectsList = repository.getProjects();
            Collections.reverse(projectsList);
            projects.postValue(projectsList);
        }).start();
    }

    public Project getProject(int id) {
        return repository.getProject(id);
    }

    public void loadTaskByID(int taskID) {
        task.setValue(repository.getTaskById(taskID));
        isTaskBeingEdited.setValue(true);
        if (task.getValue().repeatValue == 1)   // every week repeat
            everyWeekEvent.call();
        isThisTaskUpdating = true;
    }

//    Handlers events from xml.

    public void setCheckboxEvent(boolean isChecked) {
        regularTaskEvent.postValue(!isChecked);
        task.getValue().isRegularTask = !isChecked;
    }

    public void setRepeatType(int position) {
        repeatType.setValue(position);
        task.getValue().repeatValue = position;
    }

    public void openEstimateDateCalendar() {
        endDateCalendarEvent.call();
    }

    public void openExactlyDateCalendar() {
        repeatDateCalendarEvent.call();
    }

    public void openProjectsDialog() {
        projectDialogEvent.call();
    }

    public void saveTask() {
        boolean isFieldsHaveErrors = false;

        if (task.getValue().name == null || task.getValue().name.equals("")
                ||task.getValue().name.trim().isEmpty()) {
            nameFieldErrorEvent.call();
            isFieldsHaveErrors = true;
        }

        if (task.getValue().isRegularTask && task.getValue().repeatValue == 1 && task.getValue().pickedDays.equals("")) {
            dayPickerErrorEvent.call();
            isFieldsHaveErrors = true;
        }

        if (task.getValue().projectID == null) {
            emptyProjectErrorEvent.call();
            isFieldsHaveErrors = true;
        }

        if (isFieldsHaveErrors)
            return;

        if (task.getValue().estimateTime == null)
            task.getValue().estimateTime = 0f;

        if (task.getValue().estimateTime != null && task.getValue().estimateTime != 0f)
            task.getValue().estimateTime = Float.valueOf(String.format(Locale.ENGLISH, "%.2f", task.getValue().estimateTime));

        if (task.getValue().repeatValue == 2 && (task.getValue().repeatDate.isEmpty() || task.getValue().repeatDate.equals(" "))) {
            repeatDateErrorEvent.call();
            return;
        }

        if(task.getValue().isRegularTask) {
            if(task.getValue().repeatValue == 0) {
                task.getValue().isActive = true;
            } else if(task.getValue().repeatValue == 1) {
                GregorianCalendar cal = new GregorianCalendar();
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                if (task.getValue().pickedDays.contains(String.valueOf(dayOfWeek))) {
                    task.getValue().isActive = true;
                }
            } else if(task.getValue().repeatValue == 2) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
                Date todayData = new Date(System.currentTimeMillis());
                String currentDate = simpleDateFormat.format(todayData);
                if(task.getValue().repeatDate.equals(currentDate)) {
                    task.getValue().isActive = true;
                }
            }
        }

        task.getValue().name = task.getValue().name.trim();
        task.getValue().description = task.getValue().description.trim();
        if (isThisTaskUpdating) {
            repository.updateTask(task.getValue());
        } else {
            repository.insertTask(task.getValue());
        }
        saveEvent.call();
    }

//    Getters for LiveData.

    public MutableLiveData<Boolean> getIsTaskBeingEdited() {
        return isTaskBeingEdited;
    }

    public MutableLiveData<Integer> getRepeatType() {
        return repeatType;
    }

//    Getters for event listeners.

    public SingleLiveEvent<Void> getRepeatDateCalendarEvent() {
        return repeatDateCalendarEvent;
    }

    public SingleLiveEvent<Void> getDayPickerErrorEvent() {
        return dayPickerErrorEvent;
    }

    public SingleLiveEvent<Void> getNameFieldErrorEvent() {
        return nameFieldErrorEvent;
    }

    public SingleLiveEvent<Void> getEmptyProjectErrorEvent() {
        return emptyProjectErrorEvent;
    }

    public SingleLiveEvent<Void> getRepeatDateErrorEvent() {
        return repeatDateErrorEvent;
    }

    public SingleLiveEvent<Void> getEveryWeekEvent() {
        return everyWeekEvent;
    }

    public SingleLiveEvent<Void> getProjectsDialogEvent() {
        return projectDialogEvent;
    }

    public SingleLiveEvent<Void> getEndDateCalendarEvent() {
        return endDateCalendarEvent;
    }

    public SingleLiveEvent<Void> getSaveEvent() {
        return saveEvent;
    }

    public SingleLiveEvent<Boolean> getRegularTaskEvent() {
        return regularTaskEvent;
    }

}

