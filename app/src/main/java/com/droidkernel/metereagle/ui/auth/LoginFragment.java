package com.droidkernel.metereagle.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.droidkernel.metereagle.R;
import com.droidkernel.metereagle.databinding.FragmentLoginBinding;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Login screen prototype.
 *
 * <p>UI only for now: it validates the form locally and stops there. Slice 2
 * swaps {@link #attemptLogin()}'s Toast for the real {@code POST /api/auth/login}
 * call and navigates on success.
 */
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private boolean wasImeVisible;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        applyWindowInsets();
        clearErrorWhileTyping(binding.emailInput, binding.emailInputLayout);
        clearErrorWhileTyping(binding.passwordInput, binding.passwordInputLayout);

        binding.loginButton.setOnClickListener(v -> attemptLogin());

        binding.passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            // A hardware Enter delivers both ACTION_DOWN and ACTION_UP here; without
            // this guard the form submits twice.
            boolean isDone = actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getAction() == KeyEvent.ACTION_DOWN);
            if (isDone) {
                attemptLogin();
            }
            return isDone;
        });

        binding.forgotPasswordText.setOnClickListener(v ->
                toast("Password reset comes with the auth slice"));

        // TODO(slice-2): replace with
        //   Navigation.findNavController(v).navigate(R.id.action_login_to_register);
        // once RegisterFragment is in the nav graph.
        binding.signUpButton.setOnClickListener(v ->
                toast("Registration screen not wired up yet"));
    }

    /**
     * The artwork runs edge to edge, so the scrolling content — not the root —
     * carries the padding: clear of the toolbar on top, clear of the gesture bar
     * or the keyboard (whichever is taller) on the bottom.
     */
    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollView, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(
                    bars.left,
                    bars.top + actionBarSizePx(),
                    bars.right,
                    Math.max(bars.bottom, ime.bottom));

            // Padding shrinks the usable area but never moves the scroll position,
            // so the Log in button would sit under the keyboard. Edge-to-edge makes
            // the IME our problem: scroll the form back into reach ourselves.
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            if (imeVisible && !wasImeVisible) {
                v.post(() -> {
                    if (binding != null) {
                        binding.scrollView.smoothScrollTo(0, binding.loginButton.getBottom());
                    }
                });
            }
            wasImeVisible = imeVisible;
            return insets;
        });
    }

    private int actionBarSizePx() {
        TypedValue value = new TypedValue();
        if (requireContext().getTheme()
                .resolveAttribute(androidx.appcompat.R.attr.actionBarSize, value, true)) {
            return TypedValue.complexToDimensionPixelSize(
                    value.data, getResources().getDisplayMetrics());
        }
        return 0;
    }

    private void attemptLogin() {
        String email = textOf(binding.emailInput);
        String password = textOf(binding.passwordInput);
        boolean valid = true;

        if (email.isEmpty()) {
            binding.emailInputLayout.setError(getString(R.string.login_error_email_empty));
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.setError(getString(R.string.login_error_email_invalid));
            valid = false;
        }

        if (password.isEmpty()) {
            binding.passwordInputLayout.setError(getString(R.string.login_error_password_empty));
            valid = false;
        }

        if (!valid) {
            return;
        }

        boolean rememberMe = binding.rememberMeCheckBox.isChecked();
        toast("Would sign in " + email + (rememberMe ? " (remembered)" : ""));
    }

    private static String textOf(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    /** Errors clear on the next keystroke rather than sticking until re-submit. */
    private static void clearErrorWhileTyping(EditText field, TextInputLayout layout) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Fragment outlives its view; drop the binding or it leaks the whole tree.
        binding = null;
    }
}
