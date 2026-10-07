package com.simoesctt.marsclock

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.simoesctt.marsclock.data.AppDb
import com.simoesctt.marsclock.data.Task
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var mtcTime: TextView
    private lateinit var earthUtc: TextView
    private lateinit var todaySol: TextView
    private lateinit var yearHeader: TextView
    private lateinit var monthsContainer: LinearLayout
    private lateinit var curiositySol: TextView
    private lateinit var perseveranceSol: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var currentYear = 0
    private var todayAbsolute = 0L
    private var pendingSols: Set<Long> = emptySet()

    private val notifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val tick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mtcTime = findViewById(R.id.mtcTime)
        earthUtc = findViewById(R.id.earthUtc)
        todaySol = findViewById(R.id.todaySol)
        yearHeader = findViewById(R.id.yearHeader)
        monthsContainer = findViewById(R.id.monthsContainer)
        curiositySol = findViewById(R.id.curiositySol)
        perseveranceSol = findViewById(R.id.perseveranceSol)

        val d = MarsTime.darian(MarsTime.marsSolDate(System.currentTimeMillis()))
        currentYear = d.year
        todayAbsolute = d.absoluteSol

        yearHeader.setOnClickListener { showYearJump() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        lifecycleScope.launch {
            AppDb.get(this@MainActivity).tasks().pendingCounts().collectLatest { list ->
                pendingSols = list.map { it.solDate }.toSet()
                renderCalendar()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(tick)
        renderCalendar()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    private fun updateClock() {
        val now = System.currentTimeMillis()
        mtcTime.text = MarsTime.mtcString(now)
        earthUtc.text = MarsTime.earthUtcShort(now)
        val d = MarsTime.darian(MarsTime.marsSolDate(now))
        todaySol.text = String.format(Locale.US, "Today: Sol %d, %s  MY %d",
            d.sol, MarsTime.monthName(d.month), d.year)
        curiositySol.text = "Curiosity: Sol ${MarsTime.curiositySol(now)}"
        perseveranceSol.text = "Perseverance: Sol ${MarsTime.perseveranceSol(now)}"
    }

    private fun renderCalendar() {
        yearHeader.text = "Mars Year $currentYear  (tap to jump)"
        CalendarRenderer.renderYear(this, monthsContainer, currentYear, todayAbsolute, pendingSols) { absSol ->
            showSolDialog(absSol)
        }
    }

    private fun showYearJump() {
        val years = (currentYear - 5..currentYear + 5).toList()
        val labels = years.map { "MY $it" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Jump to Mars Year")
            .setItems(labels) { _, which -> currentYear = years[which]; renderCalendar() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSolDialog(absSol: Long) {
        val dao = AppDb.get(this).tasks()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }
        val d = MarsTime.darian(absSol.toDouble())
        val header = TextView(this).apply {
            text = String.format(Locale.US, "Sol %d, %s  MY %d", d.sol, MarsTime.monthName(d.month), d.year)
            setTextSize(16f)
            setPadding(0, 0, 0, 20)
        }
        container.addView(header)
        val taskList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        container.addView(taskList)
        val addBtn = Button(this).apply { text = "Add task" }
        container.addView(addBtn)

        val dialog = AlertDialog.Builder(this)
            .setView(container)
            .setNegativeButton("Close", null)
            .create()

        addBtn.setOnClickListener { promptNewTask(absSol) { dialog.dismiss() } }

        lifecycleScope.launch {
            dao.forSol(absSol).collectLatest { tasks ->
                taskList.removeAllViews()
                if (tasks.isEmpty()) {
                    taskList.addView(TextView(this@MainActivity).apply {
                        text = "(no tasks)"; setPadding(0, 10, 0, 10)
                    })
                } else {
                    tasks.forEach { task -> taskList.addView(buildTaskRow(task, dao)) }
                }
            }
        }
        dialog.show()
    }

    private fun buildTaskRow(task: Task, dao: com.simoesctt.marsclock.data.TaskDao): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
        }
        val cb = CheckBox(this).apply {
            isChecked = task.done
            setOnCheckedChangeListener { _, checked ->
                lifecycleScope.launch {
                    val updated = task.copy(done = checked)
                    dao.update(updated)
                    if (checked && task.recurrenceSols > 0) {
                        val next = updated.copy(
                            id = 0,
                            solDate = updated.solDate + updated.recurrenceSols,
                            done = false
                        )
                        dao.insert(next)
                        ReminderScheduler.schedule(this@MainActivity, next)
                    }
                }
            }
        }
        val label = TextView(this).apply {
            text = buildString {
                if (task.mtcHour >= 0) append(String.format(Locale.US, "%02d:%02d  ", task.mtcHour, task.mtcMinute))
                append(task.title)
                if (task.remind) append("  🔔")
                if (task.recurrenceSols > 0) append("  ↻ every ${task.recurrenceSols} sols")
                if (task.note.isNotBlank()) append("\n  ${task.note}")
            }
            setPadding(20, 0, 20, 0)
            setOnLongClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage("Delete this task?")
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch {
                            ReminderScheduler.cancel(this@MainActivity, task)
                            dao.delete(task)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
        }
        row.addView(cb)
        row.addView(label)
        return row
    }

    private fun promptNewTask(absSol: Long, onAdded: () -> Unit) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }
        val titleInput = EditText(this).apply { hint = "Task title" }
        val noteInput = EditText(this).apply { hint = "Note (optional)" }
        val hourInput = EditText(this).apply { hint = "MTC hour 0-23 (optional)" }
        val minuteInput = EditText(this).apply { hint = "MTC minute 0-59" }
        val recurInput = EditText(this).apply { hint = "Repeat every N sols (0 = none)" }
        val remindCb = CheckBox(this).apply { text = "Remind me" }

        container.addView(titleInput)
        container.addView(noteInput)
        container.addView(hourInput)
        container.addView(minuteInput)
        container.addView(recurInput)
        container.addView(remindCb)

        AlertDialog.Builder(this)
            .setTitle("New task")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val t = titleInput.text.toString().trim()
                if (t.isEmpty()) { Toast.makeText(this, "Title required", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val h = hourInput.text.toString().trim().toIntOrNull() ?: -1
                val m = minuteInput.text.toString().trim().toIntOrNull() ?: -1
                val r = recurInput.text.toString().trim().toIntOrNull() ?: 0
                val task = Task(
                    solDate = absSol,
                    title = t,
                    note = noteInput.text.toString().trim(),
                    mtcHour = if (h in 0..23) h else -1,
                    mtcMinute = if (m in 0..59) m else -1,
                    remind = remindCb.isChecked && h >= 0,
                    recurrenceSols = if (r > 0) r else 0
                )
                lifecycleScope.launch {
                    AppDb.get(this@MainActivity).tasks().insert(task)
                    if (task.remind) ReminderScheduler.schedule(this@MainActivity, task)
                    Toast.makeText(this@MainActivity, "Saved", Toast.LENGTH_SHORT).show()
                    onAdded()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
