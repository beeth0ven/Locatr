package cn.beeth0ven.locatr;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.squareup.picasso.Picasso;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by Air on 2017/2/19.
 */

public class LocatrFragment extends SupportMapFragment {
    private GoogleApiClient client;
    private GoogleMap map;
    private MenuItem locateMenuItem;
    private PublishSubject<Location> rxCurrentLocation = PublishSubject.create();
    private Gallery gallery;
    private Location currentLocation;

    public static LocatrFragment newInstance() {
        return new LocatrFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        client = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        getActivity().invalidateOptionsMenu();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .build();

        rxCurrentLocation.flatMap(FlickrFetchr::searchGalleries)
                .filter(galleries -> !galleries.isEmpty())
                .map(galleries -> galleries.get(0))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(gallery1 -> {
                        gallery = gallery1;
                        updateUI();
                }
        );

        getMapAsync(googleMap -> {
                    map = googleMap;
                    updateUI();
                }
        );
    }

    @Override
    public void onStart() {
        super.onStart();

        getActivity().invalidateOptionsMenu();
        client.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        client.disconnect();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.locatr_fragment, menu);

        locateMenuItem = menu.findItem(R.id.locateMenuItem);
        locateMenuItem.setEnabled(client.isConnected());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.locateMenuItem:
                findImage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void findImage() {
        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setNumUpdates(1);
        request.setInterval(0);
        if (ActivityCompat.checkSelfPermission(
                getActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi
                .requestLocationUpdates(client, request, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        Log.i("LocatrFragment", "Got a fix: " + location);
                        rxCurrentLocation.onNext(location);
                        currentLocation = location;
                        updateUI();
                    }
                });
    }

    private void updateUI() {
        if (map == null || gallery == null) {
            return;
        }

        LatLng galleryPoint = new LatLng(gallery.latitude, gallery.longitude);
        LatLng currentPoint = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        // 将图片显示到地图上,目前暂不实现
//        BitmapDescriptor itemBitmap = BitmapDescriptorFactory.fromBitmap(mMapImage);
//        MarkerOptions itemMarker = new MarkerOptions()
//                .position(itemPoint)
//                .icon(itemBitmap);
//        MarkerOptions myMarker = new MarkerOptions()
//                .position(myPoint);
//
//        mMap.clear();
//        mMap.addMarker(itemMarker);
//        mMap.addMarker(myMarker);

        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(galleryPoint)
                .include(currentPoint)
                .build();

        int margin = getResources().getDimensionPixelSize(R.dimen.map_inset_margin);
        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, margin);
        map.animateCamera(update);
    }
}
