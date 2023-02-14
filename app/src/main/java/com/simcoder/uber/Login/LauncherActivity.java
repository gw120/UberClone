package com.simcoder.uber.Login;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;
import com.simcoder.uber.Customer.CustomerMapActivity;
import com.simcoder.uber.Driver.DriverMapActivity;
import com.simcoder.uber.R;
import com.stripe.android.PaymentConfiguration;

import org.jetbrains.annotations.NotNull;

/**
 * First activity of the app.
 * <p>
 * Responsible for checking if the user is logged in or not and call
 * the AuthenticationActivity or MainActivity depending on that.
 */
public class LauncherActivity extends AppCompatActivity {

    int triedTypes = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            checkUserAccType();
        } else {
            Intent intent = new Intent(LauncherActivity.this, AuthenticationActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }


    /**
     * Check user account type, either customer or driver.
     * If it doesn't have a type then start the DetailsActivity for the
     * user to be able to pick one.
     */
    private void checkUserAccType() {
        String userID;

        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userID);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    startApis("Customers");
                    Intent intent = new Intent(getApplication(), CustomerMapActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    checkNoType();
                }
            }

            @Override
            public void onCancelled(@NotNull DatabaseError databaseError) {
            }
        });
        DatabaseReference mDriverDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userID);
        mDriverDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    startApis("Drivers");
                    Intent intent = new Intent(getApplication(), DriverMapActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    checkNoType();
                }
            }

            @Override
            public void onCancelled(@NotNull DatabaseError databaseError) {
            }
        });
    }

    /**
     * checks if both types have not been fetched meaning the DetailsActivity must be called
     */
    void checkNoType() {
        triedTypes++;
        if (triedTypes == 2) {
            Intent intent = new Intent(getApplication(), DetailsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    /**
     * starts up onesignal and stripe apis
     * @param type - type of the user (customer, driver)
     */
    void startApis(String type) {
        OneSignal.startInit(this).init();
        OneSignal.sendTag("User_ID", FirebaseAuth.getInstance().getCurrentUser().getUid());
        OneSignal.setEmail(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        //OneSignal.setInFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification);
        OneSignal.idsAvailable((userId, registrationId) -> FirebaseDatabase.getInstance().getReference().child("Users").child(type).child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("notificationKey").setValue(userId));
        PaymentConfiguration.init(
                getApplicationContext(),
                getResources().getString(R.string.publishablekey)
        );
    }
}
 122  
android/app/src/main/java/com/simcoder/uber/Login/LoginFragment.java
@@ -0,0 +1,122 @@
package com.simcoder.uber.Login;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.simcoder.uber.R;

import org.jetbrains.annotations.NotNull;

/**
 * Fragment Responsible for Logging in an existing user
 */
public class LoginFragment extends Fragment implements View.OnClickListener {

    private EditText mEmail, mPassword;


    private View view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null)
            view = inflater.inflate(R.layout.fragment_login, container, false);
        else
            container.removeView(view);


        return view;
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeObjects();
    }

    /**
     * Sends an email to the email that's on the email input for the user to reset the password
     */
    private void forgotPassword() {
        if (mEmail.getText().toString().trim().length() > 0)
            FirebaseAuth.getInstance().sendPasswordResetEmail(mEmail.getText().toString())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Snackbar.make(view.findViewById(R.id.layout), "Email Sent", Snackbar.LENGTH_LONG).show();
                        } else
                            Snackbar.make(view.findViewById(R.id.layout), "Something went wrong", Snackbar.LENGTH_LONG).show();
                    });
    }

    /**
     * Logs in the user
     */
    private void login() {
        final String email = mEmail.getText().toString();
        final String password = mPassword.getText().toString();

        if(mEmail.getText().length()==0) {
            mEmail.setError("please fill this field");
            return;
        }
        if(mPassword.getText().length()==0) {
            mPassword.setError("please fill this field");
            return;
        }

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnCompleteListener(getActivity(), task -> {
            if (!task.isSuccessful()) {
                Snackbar.make(view.findViewById(R.id.layout), "sign in error", Snackbar.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.forgotButton:
                forgotPassword();
                break;
            case R.id.login:
                login();
                break;
        }
    }


    /**
     * Initializes the design Elements and calls clickListeners for them
     */
    private void initializeObjects() {
        mEmail = view.findViewById(R.id.email);
        mPassword = view.findViewById(R.id.password);
        TextView mForgotButton = view.findViewById(R.id.forgotButton);
        Button mLogin = view.findViewById(R.id.login);


        mForgotButton.setOnClickListener(this);
        mLogin.setOnClickListener(this);

    }
}