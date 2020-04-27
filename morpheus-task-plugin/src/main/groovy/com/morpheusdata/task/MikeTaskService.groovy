package com.morpheusdata.task

import com.morpheusdata.core.ExecutableTaskInterface
import com.morpheusdata.model.ComputeServer
import com.morpheusdata.model.Container
import com.morpheusdata.model.Task
import com.morpheusdata.model.TaskResult

class MikeTaskService implements ExecutableTaskInterface {

	@Override
	TaskResult executeLocalTask(Task task, Map opts, Container container, ComputeServer server) {
		executeTask(task)
	}

	@Override
	TaskResult executeServerTask(ComputeServer server, Task task, Map opts) {
		executeTask(task)
	}

	@Override
	TaskResult executeServerTask(ComputeServer server, Task task) {
		executeTask(task)
	}

	@Override
	TaskResult executeContainerTask(Container container, Task task, Map opts) {
		println container.hostname
		executeTask(task)
	}

	@Override
	TaskResult executeContainerTask(Container container, Task task) {
		executeTask(task)
	}

	@Override
	TaskResult executeRemoteTask(Task task, Map opts, Container container, ComputeServer server) {
		executeTask(task)
	}

	@Override
	TaskResult executeRemoteTask(Task task, Container container, ComputeServer server) {
		return null
	}

	TaskResult executeTask(Task task) {
		def taskOption = task.taskOptions.find { it.optionType.code == 'mikeTaskText' }
		String data = taskOption?.value
		new TaskResult(
				success: true,
				data   : data,
				output : data.reverse()
		)
	}
}