package com.ankit.custompiedemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.ankit.custompiedemo.views.PieCircleView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PieCircleView pieCircleView = (PieCircleView) findViewById(R.id.pieChartView);
        pieCircleView.setData(getItemList());
        pieCircleView.setOnItemClickListeners(new PieCircleView.OnItemCliCkListener() {
            @Override
            public void onItemClick(int pos) {
                Toast.makeText(MainActivity.this, "Pos -> " + pos, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private List<PieCircleView.Item> getItemList() {
        List<PieCircleView.Item> items = new ArrayList<>();
        items.add(new PieCircleView.Item(R.color.item_one_color, R.drawable.space));
        items.add(new PieCircleView.Item(R.color.item_two_color, R.drawable.space));
        items.add(new PieCircleView.Item(R.color.item_three_color, R.drawable.space));
        items.add(new PieCircleView.Item(R.color.item_four_color, R.drawable.space));
        items.add(new PieCircleView.Item(R.color.item_five_color, R.drawable.space));
        items.add(new PieCircleView.Item(R.color.item_six_color, R.drawable.space));
        items.add(new PieCircleView.Item(R.color.item_seven_color, R.drawable.space));
        items.add(new PieCircleView.Item(R.color.item_eight_color, R.drawable.space));
        return items;
    }

}
