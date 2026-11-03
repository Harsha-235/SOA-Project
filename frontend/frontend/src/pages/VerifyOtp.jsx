import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  LibraryBig,
  Mail,
  Phone,
  CheckCircle2,
} from "lucide-react";
import authService from "../services/authService";

function VerifyOtp() {
  const location = useLocation();
  const navigate = useNavigate();

  const email = location.state?.email || "";
  const mobileNumber =
    location.state?.mobileNumber || "";

  const [step, setStep] = useState("email");
  const [otp, setOtp] = useState("");

  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();

    setError("");
    setSuccess("");
    setLoading(true);

    try {
      // -----------------------------------------
      // EMAIL OTP
      // -----------------------------------------
      if (step === "email") {
        await authService.verifyEmailOtp(
          email,
          otp
        );

        setOtp("");

        // Automatically move to mobile verification
        setStep("mobile");

        setSuccess(
          "Email verified successfully. Now verify your mobile number."
        );
      }

      // -----------------------------------------
      // MOBILE OTP
      // -----------------------------------------
      else {
        await authService.verifyMobileOtp(
          email,
          otp
        );

        setOtp("");

        // Show account created screen
        setSuccess("");

        setAccountCreated(true);
      }
    } catch (error) {
      console.error(
        "OTP verification failed:",
        error
      );

      const message =
        error.response?.data?.message ||
        "Invalid or expired OTP.";

      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    setError("");
    setSuccess("");
    setResending(true);

    try {
      if (step === "email") {
        await authService.resendEmailOtp(email);

        setSuccess(
          "A new OTP has been sent to your email address."
        );
      } else {
        await authService.resendMobileOtp(email);

        setSuccess(
          "A new OTP has been sent to your mobile number."
        );
      }
    } catch (error) {
      console.error(
        "Resend OTP failed:",
        error
      );

      const message =
        error.response?.data?.message ||
        "Failed to resend OTP.";

      setError(message);
    } finally {
      setResending(false);
    }
  };

  const [accountCreated, setAccountCreated] =
    useState(false);

  // -----------------------------------------
  // ACCOUNT CREATED SUCCESS SCREEN
  // -----------------------------------------
  if (accountCreated) {
    return (
      <main className="login-page">
        <section className="login-card">

          <div className="login-header">
            <div className="login-logo">
              <LibraryBig
                size={25}
                strokeWidth={1.7}
              />
            </div>

            <div className="login-brand">
              <h1>Archivalia</h1>
              <span>Digital E-Library</span>
            </div>
          </div>

          <div
            style={{
              display: "flex",
              justifyContent: "center",
              marginTop: "25px",
              marginBottom: "20px",
            }}
          >
            <CheckCircle2
              size={64}
              strokeWidth={1.5}
              style={{
                color: "#16a34a",
              }}
            />
          </div>

          <div
            className="login-heading"
            style={{
              textAlign: "center",
            }}
          >
            <span className="section-label">
              REGISTRATION COMPLETE
            </span>

            <h2>
              Account created successfully
            </h2>

            <p>
              Your Archivalia account has been
              created successfully.
            </p>

            <p>
              Your email and mobile number have
              been verified.
            </p>
          </div>

          <button
            type="button"
            className="login-button"
            onClick={() => navigate("/login")}
          >
            <span>Go to Login</span>
          </button>

          <div className="login-divider">
            <span>ACCOUNT READY</span>
          </div>

          <button
            type="button"
            className="register-button"
            onClick={() => navigate("/login")}
          >
            Click here to sign in
          </button>

          <p className="security-note">
            Your account is ready · Welcome to Archivalia
          </p>

        </section>
      </main>
    );
  }

  // -----------------------------------------
  // OTP VERIFICATION SCREEN
  // -----------------------------------------
  return (
    <main className="login-page">
      <section className="login-card">

        {/* Header */}
        <div className="login-header">
          <div className="login-logo">
            <LibraryBig
              size={25}
              strokeWidth={1.7}
            />
          </div>

          <div className="login-brand">
            <h1>Archivalia</h1>
            <span>Digital E-Library</span>
          </div>
        </div>

        {/* Heading */}
        <div className="login-heading">

          <span className="section-label">
            {step === "email"
              ? "EMAIL VERIFICATION"
              : "MOBILE VERIFICATION"}
          </span>

          <h2>
            {step === "email"
              ? "Verify your email"
              : "Verify your mobile"}
          </h2>

          <p>
            {step === "email"
              ? "Enter the OTP sent to your email address."
              : "Enter the OTP sent to your mobile number."}
          </p>

        </div>

        {/* Contact Details */}
        <div
          style={{
            background: "#f8fafc",
            border: "1px solid #e2e8f0",
            borderRadius: "8px",
            padding: "12px 14px",
            marginBottom: "18px",
            fontSize: "13px",
          }}
        >

          {/* Email */}
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "8px",
              marginBottom:
                step === "mobile"
                  ? "8px"
                  : "0",
            }}
          >
            <Mail size={16} />

            <span>
              <strong>Email:</strong>{" "}
              {email}
            </span>
          </div>

          {/* Mobile */}
          {step === "mobile" && (
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "8px",
              }}
            >
              <Phone size={16} />

              <span>
                <strong>Mobile:</strong>{" "}
                {mobileNumber}
              </span>
            </div>
          )}

        </div>

        {/* Error */}
        {error && (
          <p
            style={{
              color: "#dc2626",
              textAlign: "center",
              marginBottom: "15px",
              fontSize: "14px",
            }}
          >
            {error}
          </p>
        )}

        {/* Success */}
        {success && (
          <p
            style={{
              color: "#16a34a",
              textAlign: "center",
              marginBottom: "15px",
              fontSize: "14px",
            }}
          >
            {success}
          </p>
        )}

        {/* OTP Form */}
        <form
          onSubmit={handleSubmit}
          className="login-form"
        >

          <div className="form-group">

            <label htmlFor="otp">
              {step === "email"
                ? "Email OTP"
                : "Mobile OTP"}
            </label>

            <div className="input-wrapper">

              {step === "email" ? (
                <Mail size={18} />
              ) : (
                <Phone size={18} />
              )}

              <input
                id="otp"
                type="text"
                inputMode="numeric"
                maxLength="6"
                placeholder="Enter 6-digit OTP"
                value={otp}
                onChange={(e) =>
                  setOtp(
                    e.target.value.replace(
                      /\D/g,
                      ""
                    )
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
                : step === "email"
                ? "Verify email"
                : "Verify mobile"}
            </span>
          </button>

        </form>

        {/* Resend OTP */}
        <button
          type="button"
          className="register-button"
          onClick={handleResend}
          disabled={resending}
        >
          {resending
            ? "Sending..."
            : "Resend OTP"}
        </button>

        <p className="security-note">
          Secure verification · Your information
          remains protected
        </p>

      </section>
    </main>
  );
}

export default VerifyOtp;