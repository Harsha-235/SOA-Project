import { useState } from "react";
import {
  Eye,
  EyeOff,
  LockKeyhole,
  Mail,
  ShieldCheck,
  ArrowRight,
  LibraryBig,
  Check,
  X,
} from "lucide-react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import api from "../services/api";

function ResetPassword() {
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState(
    location.state?.email || ""
  );

  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] =
    useState(false);

  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);

  // =========================
  // PASSWORD REQUIREMENTS
  // =========================

  const passwordRequirements = {
    minLength: newPassword.length >= 8,
    uppercase: /[A-Z]/.test(newPassword),
    lowercase: /[a-z]/.test(newPassword),
    number: /[0-9]/.test(newPassword),
    special: /[^A-Za-z0-9]/.test(newPassword),
  };

  const isPasswordValid =
    passwordRequirements.minLength &&
    passwordRequirements.uppercase &&
    passwordRequirements.lowercase &&
    passwordRequirements.number &&
    passwordRequirements.special;

  const passwordsMatch =
    newPassword === confirmPassword &&
    confirmPassword.length > 0;

  // =========================
  // REQUIREMENT COMPONENT
  // =========================

  const Requirement = ({ valid, children }) => (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        gap: "6px",
        fontSize: "12px",
        color: valid ? "#16a34a" : "#64748b",
        marginBottom: "4px",
      }}
    >
      {valid ? (
        <Check size={13} />
      ) : (
        <X size={13} />
      )}

      <span>{children}</span>
    </div>
  );

  // =========================
  // VERIFY OTP
  // =========================

  const handleVerifyOtp = async (e) => {
    e.preventDefault();

    if (!email.trim()) {
      alert("Please enter your email address.");
      return;
    }

    if (!otp.trim()) {
      alert("Please enter the OTP.");
      return;
    }

    try {
      setLoading(true);

      await api.post("/auth/verify-password-reset-otp", {
        email: email.trim(),
        otp: otp.trim(),
      });

      alert("OTP verified successfully.");

      setStep(2);
    } catch (error) {
      console.error("OTP verification failed:", error);

      const message =
        error.response?.data?.message ||
        error.response?.data ||
        "Invalid or expired OTP.";

      alert(message);
    } finally {
      setLoading(false);
    }
  };

  // =========================
  // RESET PASSWORD
  // =========================

  const handleResetPassword = async (e) => {
    e.preventDefault();

    // Check password strength
    if (!isPasswordValid) {
      alert(
        "Password must contain at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character."
      );
      return;
    }

    // Check passwords match
    if (!passwordsMatch) {
      alert("Passwords do not match.");
      return;
    }

    try {
      setLoading(true);

      await api.post("/auth/reset-password", {
        email: email.trim(),
        newPassword,
      });

      alert(
        "Password reset successfully. You can now login with your new password."
      );

      navigate("/login");
    } catch (error) {
      console.error("Password reset failed:", error);

      const message =
        error.response?.data?.message ||
        error.response?.data ||
        "Unable to reset password.";

      alert(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-page">
      <section className="login-card">

        {/* HEADER */}

        <div className="login-header">
          <div className="login-logo">
            <LibraryBig size={25} strokeWidth={1.7} />
          </div>

          <div className="login-brand">
            <h1>KLU</h1>
            <span>Digital E-Library</span>
          </div>
        </div>

        {/* HEADING */}

        <div className="login-heading">
          <span className="section-label">
            {step === 1
              ? "VERIFY OTP"
              : "CREATE NEW PASSWORD"}
          </span>

          <h2>
            {step === 1
              ? "Verify OTP"
              : "Reset Password"}
          </h2>

          <p>
            {step === 1
              ? "Enter the OTP sent to your registered email address."
              : "Create a new secure password for your Archivalia account."}
          </p>
        </div>

        {/* STEP 1 — OTP */}

        {step === 1 && (
          <form
            onSubmit={handleVerifyOtp}
            className="login-form"
          >

            <div className="form-group">
              <label htmlFor="email">
                Email address
              </label>

              <div className="input-wrapper">
                <Mail size={18} />

                <input
                  id="email"
                  type="email"
                  placeholder="you@example.com"
                  value={email}
                  onChange={(e) =>
                    setEmail(e.target.value)
                  }
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="otp">
                Password reset OTP
              </label>

              <div className="input-wrapper">
                <ShieldCheck size={18} />

                <input
                  id="otp"
                  type="text"
                  inputMode="numeric"
                  maxLength={6}
                  placeholder="Enter 6-digit OTP"
                  value={otp}
                  onChange={(e) =>
                    setOtp(
                      e.target.value.replace(/\D/g, "")
                    )
                  }
                  required
                />
              </div>
            </div>

            <button
              type="submit"
              className="login-button"
              disabled={loading}
            >
              <span>
                {loading
                  ? "Verifying..."
                  : "Verify OTP"}
              </span>

              {!loading && <ArrowRight size={18} />}
            </button>

          </form>
        )}

        {/* STEP 2 — NEW PASSWORD */}

        {step === 2 && (
          <form
            onSubmit={handleResetPassword}
            className="login-form"
          >

            {/* NEW PASSWORD */}

            <div className="form-group">
              <label htmlFor="newPassword">
                New password
              </label>

              <div className="input-wrapper">
                <LockKeyhole size={18} />

                <input
                  id="newPassword"
                  type={
                    showPassword
                      ? "text"
                      : "password"
                  }
                  placeholder="Enter new password"
                  value={newPassword}
                  onChange={(e) =>
                    setNewPassword(e.target.value)
                  }
                  required
                />

                <button
                  type="button"
                  className="password-toggle"
                  onClick={() =>
                    setShowPassword(!showPassword)
                  }
                >
                  {showPassword ? (
                    <EyeOff size={18} />
                  ) : (
                    <Eye size={18} />
                  )}
                </button>
              </div>

              {/* PASSWORD REQUIREMENTS */}

              <div
                style={{
                  marginTop: "8px",
                  padding: "10px 12px",
                  background: "#f8fafc",
                  border: "1px solid #e2e8f0",
                  borderRadius: "6px",
                }}
              >
                <p
                  style={{
                    fontSize: "12px",
                    fontWeight: "600",
                    color: "#334155",
                    marginBottom: "7px",
                  }}
                >
                  Password must contain:
                </p>

                <Requirement
                  valid={passwordRequirements.minLength}
                >
                  At least 8 characters
                </Requirement>

                <Requirement
                  valid={passwordRequirements.uppercase}
                >
                  One uppercase letter (A-Z)
                </Requirement>

                <Requirement
                  valid={passwordRequirements.lowercase}
                >
                  One lowercase letter (a-z)
                </Requirement>

                <Requirement
                  valid={passwordRequirements.number}
                >
                  One number (0-9)
                </Requirement>

                <Requirement
                  valid={passwordRequirements.special}
                >
                  One special character
                </Requirement>
              </div>
            </div>

            {/* CONFIRM PASSWORD */}

            <div className="form-group">
              <label htmlFor="confirmPassword">
                Confirm new password
              </label>

              <div className="input-wrapper">
                <LockKeyhole size={18} />

                <input
                  id="confirmPassword"
                  type={
                    showConfirmPassword
                      ? "text"
                      : "password"
                  }
                  placeholder="Confirm new password"
                  value={confirmPassword}
                  onChange={(e) =>
                    setConfirmPassword(
                      e.target.value
                    )
                  }
                  required
                />

                <button
                  type="button"
                  className="password-toggle"
                  onClick={() =>
                    setShowConfirmPassword(
                      !showConfirmPassword
                    )
                  }
                >
                  {showConfirmPassword ? (
                    <EyeOff size={18} />
                  ) : (
                    <Eye size={18} />
                  )}
                </button>
              </div>

              {/* PASSWORD MATCH */}

              {confirmPassword.length > 0 && (
                <p
                  style={{
                    fontSize: "12px",
                    marginTop: "6px",
                    color: passwordsMatch
                      ? "#16a34a"
                      : "#dc2626",
                  }}
                >
                  {passwordsMatch
                    ? "✓ Passwords match"
                    : "✕ Passwords do not match"}
                </p>
              )}
            </div>

            {/* RESET BUTTON */}

            <button
              type="submit"
              className="login-button"
              disabled={loading}
            >
              <span>
                {loading
                  ? "Resetting..."
                  : "Reset Password"}
              </span>

              {!loading && <ArrowRight size={18} />}
            </button>

          </form>
        )}

        {/* BACK TO LOGIN */}

        <Link
          to="/login"
          className="register-button"
        >
          Back to Login
        </Link>

        <p className="security-note">
          Secure password recovery · Your information
          remains protected
        </p>

      </section>
    </main>
  );
}

export default ResetPassword;