package project.samp.mariusduna.twowayrecyclerview;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.AnyRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import project.samp.mariusduna.twowayrecyclerview.adapter.EpgAdapter;
import project.samp.mariusduna.twowayrecyclerview.adapter.HeaderChannelsAdapter;
import project.samp.mariusduna.twowayrecyclerview.adapter.TimelineAdapter;
import project.samp.mariusduna.twowayrecyclerview.model.ProgramModel;
import project.samp.mariusduna.twowayrecyclerview.observable.Subject;
import project.samp.mariusduna.twowayrecyclerview.utils.Utils;

public class MainActivity extends AppCompatActivity {
    private RecyclerView epgRecyclerView;
    private RecyclerView timelineRecyclerView;
    private RecyclerView channelsRecyclerView;
    private ArrayList<ProgramModel> horizontalList;
    private ArrayList<Long> timelineList;
    private ArrayList<ArrayList<ProgramModel>> verticalList;
    private EpgAdapter epgAdapter;
    private TimelineAdapter timelineAdapter;

    private ArrayList<Uri> headerChannelsList;
    private HeaderChannelsAdapter channelsAdapter;
    private Subject subject = new Subject();

    private LinearLayoutManager horizontalLayoutManagaer;
    private TextView currentTimeTextView;

    private float mDownX;
    private float mDownY;
    private long nowTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentTimeTextView = (TextView) findViewById(R.id.current_time);
        epgRecyclerView = (RecyclerView) findViewById(R.id.epg_recycler_view);
        timelineRecyclerView = (RecyclerView) findViewById(R.id.timeline_recycler_view);
        channelsRecyclerView = (RecyclerView) findViewById(R.id.channels_recycler_view);

        int hour = 3600000;
        int halfHour = hour / 2;
        int fifteenMin = hour / 4;
        int day = hour * 24;
        int week = day * 7;

        Calendar c = Calendar.getInstance();
        nowTime = c.getTimeInMillis();
        long startTime = nowTime - 2 * week;
        long endTime = nowTime + 2 * week;

        horizontalList = new ArrayList<>();
        for (long i = startTime; i <= endTime; ) {
            ProgramModel programModel = new ProgramModel();
            programModel.setStartTime(i);
            i = i + halfHour;
            programModel.setEndTime(i);
            programModel.setTitle("Title");
            programModel.setDescription("Description");
            programModel.setColorTitle(getResources().getColor(R.color.colorPrimary));
            programModel.setColorDescription(getResources().getColor(R.color.colorAccent));
            horizontalList.add(programModel);
        }

        verticalList = new ArrayList<>();
        for (int i = 0; i <= 500; i++) {
            verticalList.add(horizontalList);
        }

        timelineList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        Date date = new Date(startTime);
        calendar.setTime(date);
        int minutes = calendar.get(Calendar.MINUTE);
        int diff = 60 - minutes;
        //have only precise time from half and half an hour
        startTime = startTime + diff * 60000;

        for (long i = startTime; i <= endTime; ) {
            timelineList.add(i);
            i = i + halfHour;
        }

        headerChannelsList = new ArrayList<>();
        for (int i = 0; i <= 500; i++) {
            headerChannelsList.add(getUriToResource(getApplicationContext(), R.drawable.ic_protv));
        }

        epgAdapter = new EpgAdapter(verticalList);
        epgAdapter.setSubject(subject);

        timelineAdapter = new TimelineAdapter(timelineList);
        timelineRecyclerView.setAdapter(timelineAdapter);

        channelsAdapter = new HeaderChannelsAdapter(headerChannelsList);
        channelsRecyclerView.setAdapter(channelsAdapter);


        final LinearLayoutManager epgLayoutmanager = new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false);
        LinearLayoutManager channelsLayoutmanager = new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false);
        horizontalLayoutManagaer = new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false);
        timelineRecyclerView.setLayoutManager(horizontalLayoutManagaer);
        epgRecyclerView.setLayoutManager(epgLayoutmanager);
        channelsRecyclerView.setLayoutManager(channelsLayoutmanager);

        //start set start position for timeline recyclerview
        Date date1 = new Date(nowTime);
        DateFormat minutesFormat = new SimpleDateFormat("mm");
        DateFormat secondsFormat = new SimpleDateFormat("ss");
        long minutes1 = Integer.parseInt(minutesFormat.format(date1));
        long seconds1 = Integer.parseInt(secondsFormat.format(date1));
        long totalSeconds = TimeUnit.MINUTES.toSeconds(minutes1) + seconds1;
        //TODO here is a bug
        long diffProgramStart = TimeUnit.MINUTES.toSeconds(30) - totalSeconds;
        float diffProgramStartMillis = TimeUnit.SECONDS.toMillis(diffProgramStart);
        horizontalLayoutManagaer.scrollToPositionWithOffset(getNowPositionTimeline(), (int)Utils.convertMillisecondsToPx(diffProgramStartMillis, getApplicationContext()));
        //end set start position for timeline recyclerview

        subject.setPositionInList(getNowPositionTimeline());
        subject.setCurrentTime(nowTime);

        timelineRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                subject.setState(dx); //we should store the time
                double currentTime = subject.getCurrentTime();
                double millisPx = Utils.convertPxToMilliseconds(dx > 0 ? dx : -dx, getApplicationContext());
                double finalValue = dx > 0 ? currentTime + millisPx : currentTime - millisPx;
                subject.setCurrentTime(finalValue);
                updateCurrentTime((long)finalValue);
            }

            private void updateCurrentTime(long currentTime) {
                Date date = new Date(currentTime);
                DateFormat dateFormat = new SimpleDateFormat("EEE dd MMM hh:mm:ss");
                currentTimeTextView.setText(dateFormat.format(date));
            }
        });

        epgRecyclerView.setAdapter(epgAdapter);
    }

    private int getNowPositionTimeline() {
        int i = 0;
        while (nowTime > timelineList.get(i)) {
            i++;
        }
        return i;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = ev.getY();
                mDownX = ev.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                float y = ev.getY();
                float yDelta = y - mDownY;
                mDownY = y;

                float x = ev.getX();
                float xDelta = x - mDownX;
                mDownX = x;
                //force orthogonal movements
                if (Math.abs(yDelta) > Math.abs(xDelta)) {
                    epgRecyclerView.scrollBy(0, -(int) yDelta);
                    channelsRecyclerView.scrollBy(0, -(int) yDelta);
                } else {
                    timelineRecyclerView.scrollBy(-(int) xDelta, 0);
                }
                break;
        }
        return true;
    }

    public static final Uri getUriToResource(@NonNull Context context, @AnyRes int resId) throws Resources.NotFoundException {
        Resources res = context.getResources();
        Uri resUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + res.getResourcePackageName(resId)
                + '/' + res.getResourceTypeName(resId)
                + '/' + res.getResourceEntryName(resId));
        return resUri;
    }
}
