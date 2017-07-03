package personal.collins.com.personaltasks

import android.content.Context
import android.content.Intent

class TaskManager constructor(val context: Context) {
    fun launch(task: PersonalTask) {
        context.startActivity(task.intent)
    }
}

class PersonalTask constructor(var name: String, val intent: Intent) {
    override fun toString(): String {
        return this.name
    }
}