package ovh.adiantek.android.controlpanel_ovh;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.TreeMap;

import ovh.Api;

/**
 * Web tab
 */
public class Web extends ExpandableListView implements ExpandableListView.OnChildClickListener {
    private final Activity c;
    private ArrayList<TreeMap<String, String>> domainsList = new ArrayList<>();
    private ArrayList<TreeMap<String, String>> vpsList = new ArrayList<>();

    public Web(Activity c) {
        super(c);
        this.c = c;
        ArrayList<TreeMap<String, String>> groupData = new ArrayList<>();
        ArrayList<ArrayList<TreeMap<String, String>>> listOfChildGroups = new ArrayList<>();
        groupData.add(create("ROOT_NAME", c.getString(R.string.Domains)));
        groupData.add(create("ROOT_NAME", c.getString(R.string.Platform)));
        domainsList.add(create("CHILD_NAME", "Loading..."));
        vpsList.add(create("CHILD_NAME", "Loading..."));
        listOfChildGroups.add(domainsList);
        listOfChildGroups.add(vpsList);
        setAdapter(new SimpleExpandableListAdapter(c, groupData, android.R.layout.simple_expandable_list_item_1, new String[]{"ROOT_NAME"}, new int[]{android.R.id.text1}, listOfChildGroups, android.R.layout.simple_expandable_list_item_1, new String[]{"CHILD_NAME"}, new int[]{android.R.id.text1}));
        MyApi.createThread(c, "GET", "/domain", null, new MyApi.RunnableResponse() {
            @Override
            public void run(Api.Response response) {
                domainsList.clear();
                JSONArray jarr = (JSONArray) response.response;
                for (int i = 0; i < jarr.length(); i++)
                    try {
                        domainsList.add(create("CHILD_NAME", jarr.getString(i)));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
            }
        });
        MyApi.createThread(c, "GET", "/vps", null, new MyApi.RunnableResponse() {
            @Override
            public void run(Api.Response response) {
                vpsList.clear();
                JSONArray jarr = (JSONArray) response.response;
                for (int i = 0; i < jarr.length(); i++)
                    try {
                        vpsList.add(create("CHILD_NAME", jarr.getString(i)));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
            }
        });
        this.setOnChildClickListener(this);
    }


    private TreeMap<String, String> create(String a, String b) {
        TreeMap<String, String> map = new TreeMap<>();
        map.put(a, b);
        return map;
    }


    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        if (groupPosition == 1) {
            String serviceName = vpsList.get(childPosition).get("CHILD_NAME");
            Intent i = new Intent(c, Vps.class);
            i.putExtra("serviceName", serviceName);
            c.startActivity(i);
        }
        return false;
    }
}
