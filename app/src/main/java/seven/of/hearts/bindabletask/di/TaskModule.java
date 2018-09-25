package seven.of.hearts.bindabletask.di;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;

import seven.of.hearts.bindabletask.task.ProjectsPickerAdapter;
import seven.of.hearts.bindabletask.task.TaskFragment;
import seven.of.hearts.bindabletask.task.TaskViewModel;

import dagger.Module;
import dagger.Provides;

/*
 * Created by Eugene Zelikson (7ofHearts).
 */

@Module
public class TaskModule {

    @Provides
    TaskViewModel provideTaskViewModel (TaskFragment fragment, ViewModelProvider.Factory factory) {
        return ViewModelProviders.of(fragment, factory).get(TaskViewModel.class);
    }

    @Provides
    ProjectsPickerAdapter provideProjectsPickerAdapter(TaskFragment fragment) {
        return new ProjectsPickerAdapter(fragment);
    }
}

