package in.co.theshipper.www.shipper_driver.Fragments;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;

import in.co.theshipper.www.shipper_driver.Constants;
import in.co.theshipper.www.shipper_driver.Controller.DBController;
import in.co.theshipper.www.shipper_driver.Activities.EditProfile;
import in.co.theshipper.www.shipper_driver.Helper;
import in.co.theshipper.www.shipper_driver.Activities.FullActivity;
import in.co.theshipper.www.shipper_driver.R;


public class BillDetails extends Fragment{

    public  String TAG = EditProfile.class.getName();
    public RequestQueue requestQueue;
    private DBController controller;
    private TextView crn_no_view,vehicle_name,total_distance,total_time,total_fare_view;
    private String truck_name,crn_no = "";
    private long diffsec=0, diffmin=0, diffHours=0;
    private double totDist = 0,total_fare = 0,distance_fare=0,base_fare_min = 0,chargable_time = 0,base_fare=0;
    String  approx_distance,approx_fare;
    private int active=0;
    private Button generate_bill;
    private long loading_start_time = 0,unloading_stop_time= 0,journey_start_time =0,journey_stop_time =0, diff = 0;
    private String loading_start_time_string,unloading_stop_time_string,journey_start_time_string,journey_stop_time_string;

    public BillDetails() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if(getActivity() != null) {

            controller = new DBController(getActivity());

        }

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_bill_details, container, false);
        crn_no_view = (TextView) view.findViewById(R.id.crn_no);
        vehicle_name = (TextView) view.findViewById(R.id.vehicle_name);
        total_distance = (TextView) view.findViewById(R.id.total_distance);
        total_time = (TextView) view.findViewById(R.id.total_time);
        total_fare_view = (TextView) view.findViewById(R.id.total_fare);
        generate_bill = (Button) view.findViewById(R.id.generate_bill);
        return view;

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(getActivity() != null) {

            loading_start_time = Long.parseLong(Helper.getPreference(getActivity(),Constants.Keys.LOADING_START_TIME));
            unloading_stop_time = Long.parseLong(Helper.getPreference(getActivity(),Constants.Keys.UNLOADING_STOP_TIME));
            journey_start_time = Long.parseLong(Helper.getPreference(getActivity(),Constants.Keys.JOURNEY_START_TIME));
            journey_stop_time = Long.parseLong(Helper.getPreference(getActivity(),Constants.Keys.JOURNEY_STOP_TIME));
            loading_start_time_string = Helper.getDate(loading_start_time);
            unloading_stop_time_string = Helper.getDate(unloading_stop_time);
            journey_start_time_string = Helper.getDate(journey_start_time);
            journey_stop_time_string = Helper.getDate(journey_stop_time);
            diff = (unloading_stop_time - loading_start_time);
            diffsec = (diff / (1000) )% 60;
            diffmin = (diff / (60 * 1000)) % 60;
            diffHours = diff / (60 * 60 * 1000);

            if ((getActivity().getIntent() != null) && (getActivity().getIntent().getExtras() != null)) {

                Bundle bundle = getActivity().getIntent().getExtras();
                totDist = Double.parseDouble(Helper.getPreference(getActivity(),Constants.Keys.TOTAL_DISTANCE_TAVELLED));
                getActivity().getIntent().setData(null);
                getActivity().setIntent(null);
                crn_no = Helper.getPreference(getActivity(), Constants.Keys.CRN_NO);

                if (crn_no.length() > 0) {

                    generate_bill.setVisibility(View.VISIBLE);
                    crn_no_view.setVisibility(View.VISIBLE);

                    generate_bill.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            String update_bill_generated_url = Constants.Config.ROOT_PATH + "bill_generated";
                            HashMap<String, String> hashMap = new HashMap<String, String>();
                            hashMap.put("total_fare", String.valueOf(total_fare));
                            hashMap.put("total_time", String.valueOf(diffHours) + " hours " + String.valueOf(diffmin) + " mins " + String.valueOf(diffsec) + " secs");
                            hashMap.put("total_distance", String.valueOf(totDist));
                            hashMap.put("base_fare", String.valueOf(base_fare));
                            hashMap.put("crn_no", crn_no);
                            hashMap.put("loading_start_time", loading_start_time_string);
                            hashMap.put("unloading_end_time", unloading_stop_time_string);
                            hashMap.put("journey_start_time", journey_start_time_string);
                            hashMap.put("journey_end_time", journey_stop_time_string);
                            hashMap.put(Constants.Keys.USER_TOKEN, Helper.getPreference(getActivity(), Constants.Keys.USER_TOKEN));
                            hashMap.put(Constants.Keys.EXACT_PICKUP_POINT, Helper.getPreference(getActivity(), Constants.Keys.EXACT_PICKUP_POINT));
                            hashMap.put(Constants.Keys.EXACT_DROPOFF_POINT, Helper.getPreference(getActivity(), Constants.Keys.EXACT_DROPOFF_POINT));
                            sendVolleyRequest(update_bill_generated_url, Helper.checkParams(hashMap), "bill_generated");

                        }
                    });

                }

                calculate();

            }

        }

    }

    public void calculate() {

        int vehicletype_id = Integer.parseInt(Helper.getPreference(getContext(), Constants.Keys.VEHICLETYPE_ID));
        int city_id = Integer.parseInt(Helper.getPreference(getContext(), Constants.Keys.CITY_ID));
        Double tot_dist;
        tot_dist = totDist;
        double durationf = (double) (diffHours * 60 + diffmin);

        if(diffsec>56){

            durationf++;
        }

        SQLiteDatabase database = controller.getWritableDatabase();

        String query_truck = "SELECT " + controller.VEHICLE_NAME + " FROM " + controller.TABLE_VIEW_VEHICLE_TYPE +
                " WHERE " + controller.VEHICLETYPE_ID + " = " + vehicletype_id;

        String query1 = "SELECT " + controller.BASE_FARE + ", " +controller.TRANSIT_CHARGE+ ", " + controller.FREEWAITING_TIME + " FROM " + controller.TABLE_VIEW_BASE_FARE + " WHERE "
                + controller.VEHICLETYPE_ID + "=" + vehicletype_id+" AND " + controller.CITY_ID + " = " + city_id;

        String query2 = "SELECT " + controller.FROM_DISTANCE + ", " + controller.TO_DISTANCE + ", " + controller.PRICE_KM + " FROM " + controller.TABLE_VIEW_PRICING + " WHERE "
                + controller.VEHICLETYPE_ID + "=" + vehicletype_id +" AND " + controller.CITY_ID + " = " + city_id+ " ORDER BY " + controller.TO_DISTANCE + " ASC";

        String querycnt = "SELECT COUNT(*) FROM view_vehicle_type WHERE vehicletype_id =" + vehicletype_id;

        Cursor cnt = database.rawQuery(querycnt, null);

        if (cnt.moveToFirst()) {

            active = cnt.getInt(0);
            Helper.SystemPrintLn("to check whether the vehicle_type_id exists " + cnt.getString(0) + active);

        }

        if (active > 0) {

            try {

                Cursor q1 = database.rawQuery(query_truck, null);

                if (q1.moveToFirst()) {

                            truck_name = q1.getString(0);
                }

            } catch (Exception e) {

            }

            try {

                Cursor q1 = database.rawQuery(query1, null);

                if (q1.moveToFirst()) {

                    double freewaiting_time = q1.getDouble(2);

                    if(durationf > freewaiting_time){

                        chargable_time = (durationf-freewaiting_time);

                    }

                    base_fare = q1.getDouble(0);
                    base_fare_min = base_fare + (q1.getDouble(1) * chargable_time);
                    //base_fare_max=  (q1.getDouble(0)+(q1.getDouble(1)*durationf*2.5));

                }

            } catch (Exception e) {

            }

            try {

                Cursor q2 = database.rawQuery(query2, null);

                if (q2.moveToFirst()) {

                    do {

                        Helper.SystemPrintLn(" FROM DISTANCE, TO DISTANCE AND PRICE PER KM " + q2.getDouble(0) + ", " + q2.getFloat(1) + " AND " + q2.getFloat(2));
                        double separation = q2.getDouble(1) - q2.getDouble(0);

                        if (tot_dist > 0) {

                            if (tot_dist > separation) {

                                distance_fare = distance_fare + separation * q2.getDouble(2);
                                tot_dist = tot_dist - separation;

                            } else {

                                distance_fare = distance_fare + tot_dist * q2.getDouble(2);
                                tot_dist = (double) 0;

                            }

                        }

                    } while (q2.moveToNext());

                }

            } catch (Exception e) {

            }

            database.close();
            //total_fare_max=distance_fare+base_fare_max;
            total_fare = distance_fare + base_fare_min;
            approx_fare = Helper.getTwoDecimal(total_fare);
            approx_distance = Helper.getTwoDecimal(totDist);
            crn_no_view.setText(crn_no);
            vehicle_name.setText(truck_name);
            total_distance.setText(String.valueOf((approx_distance)+" Km."));
            total_time.setText(String.valueOf(diffHours)+" hours "+String.valueOf(diffmin)+" mins "+String.valueOf(diffsec)+" secs");
            total_fare_view.setText(String.valueOf(approx_fare+" Rs."));

        }

    }

    public void sendVolleyRequest(String URL, final HashMap<String,String> hMap,final String method){

        if(getActivity() != null) {

            StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {

                    if (method.equals("bill_generated")) {

                        String trimmed_response = response.substring(response.indexOf("{"));
                        BillGeneratesucces(trimmed_response);

                    }

                }

            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                }

            }) {

                @Override
                public HashMap<String, String> getParams() {
                    return hMap;
                }

            };

            stringRequest.setTag(TAG);
            Helper.addToRequestQue(requestQueue, stringRequest, getActivity());

        }

    }

    public void BillGeneratesucces(String response){

        if (!Helper.CheckJsonError(response)) {

            Fragment fragment = new Fragment();
            fragment = new FinishedBookingDetail();
            Bundle bundle = new Bundle();
            bundle.putString("crn_no", crn_no);
            fragment.setArguments(Helper.CheckBundle(bundle));
            FragmentManager fragmentManager = FullActivity.fragmentManager;
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.main_content, fragment, Constants.Config.CURRENT_FRAG_TAG);

            if((FullActivity.homeFragmentIndentifier == -5)){

                transaction.addToBackStack(null);
                FullActivity.homeFragmentIndentifier =  transaction.commit();

            }else{

                transaction.commit();

            }

            if(getActivity() != null) {

                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_finished_booking_detail_fragment);

            }

        }else{

            ErrorDialog(Constants.Title.SERVER_ERROR,Constants.Message.SERVER_ERROR);

        }

    }

    private void ErrorDialog(String Title,String Message){

        if(getActivity() != null) {

            Helper.showDialog(getActivity(), Title, Message);

        }

    }

    @Override
    public void onPause() {
        super.onPause();

        Helper.stopAllVolley(requestQueue);

    }

    @Override
    public void onResume() {
        super.onResume();

        Helper.startAllVolley(requestQueue);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        Helper.cancelAllRequest(requestQueue,TAG);

    }

}
