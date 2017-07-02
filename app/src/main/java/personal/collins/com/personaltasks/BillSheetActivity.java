package personal.collins.com.personaltasks;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class BillSheetActivity extends AppCompatActivity {
    private int selectedMonth = 1;
    private int selectedYear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_sheet);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Spinner months = (Spinner) findViewById(R.id.monthSpinner);
        Spinner years = (Spinner) findViewById(R.id.yearSpinner);
        Button generate = (Button) findViewById(R.id.generateButton);

        months.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedMonth = i + 1;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        int year = Calendar.getInstance().get(Calendar.YEAR);

        final ArrayList<Integer> spinnerYears = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            spinnerYears.add(year + i);
        }

        ArrayAdapter<Integer> yearAdapter = new ArrayAdapter<Integer>(
                getApplicationContext(),
                android.R.layout.simple_spinner_dropdown_item,
                spinnerYears
        );

        years.setAdapter(yearAdapter);

        years.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedYear = spinnerYears.get(adapterView.getSelectedItemPosition());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(BillSheetActivity.this, String.format("Month: %3d, Year: %03d", selectedMonth, selectedYear), Toast.LENGTH_SHORT).show();

                
            }
        });
    }

}
