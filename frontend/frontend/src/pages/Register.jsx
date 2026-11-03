import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  LibraryBig,
  Mail,
  LockKeyhole,
  User,
  Phone,
  Check,
  X,
} from "lucide-react";
import authService from "../services/authService";

function Register() {
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [mobileNumber, setMobileNumber] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Password requirements
  const passwordRequirements = {
    minLength: password.length >= 8,
    uppercase: /[A-Z]/.test(password),
    lowercase: /[a-z]/.test(password),
    number: /[0-9]/.test(password),
    special: /[^A-Za-z0-9]/.test(password),
  };

  const isPasswordValid =
    passwordRequirements.minLength &&
    passwordRequirements.uppercase &&
    passwordRequirements.lowercase &&
    passwordRequirements.number &&
    passwordRequirements.special;

  const passwordsMatch =
    password === confirmPassword &&
    confirmPassword.length > 0;

  const handleSubmit = async (e) => {
    e.preventDefault();

    setError("");

    // Validate password requirements
    if (!isPasswordValid) {
      setError(
        "Password must contain at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character."
      );
      return;
    }

    // Validate confirm password
    if (!passwordsMatch) {
      setError("Passwords do not match.");
      return;
    }

    setLoading(true);

    try {
      await authService.register(
        name,
        email,
        mobileNumber,
        password
      );

      // Pass email and mobile number to OTP page
      navigate("/verify-otp", {
        state: {
          email,
          mobileNumber,
        },
      });
    } catch (error) {
      console.error("Registration failed:", error);

      const message =
        error.response?.data?.message ||
        "Registration failed. Please try again.";

      setError(message);
    } finally {
      setLoading(false);
    }
  };

  // Requirement indicator
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
            MEMBER REGISTRATION
          </span>

          <h2>Create account</h2>

          <p>
            Register for your Archivalia library account
            and access digital learning resources.
          </p>
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

        <form
          onSubmit={handleSubmit}
          className="login-form"
        >

          {/* Full Name */}
          <div className="form-group">
            <label htmlFor="name">
              Full name
            </label>

            <div className="input-wrapper">
              <User size={18} />

              <input
                id="name"
                type="text"
                placeholder="Enter your full name"
                value={name}
                onChange={(e) =>
                  setName(e.target.value)
                }
                required
              />
            </div>
          </div>

          {/* Email */}
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

          {/* Mobile Number */}
          <div className="form-group">
            <label htmlFor="mobileNumber">
              Mobile number
            </label>

            <div className="input-wrapper">
              <Phone size={18} />

              <input
                id="mobileNumber"
                type="tel"
                inputMode="numeric"
                placeholder="Enter your mobile number"
                value={mobileNumber}
                onChange={(e) =>
                  setMobileNumber(
                    e.target.value.replace(/\D/g, "")
                  )
                }
                maxLength="10"
                required
              />
            </div>
          </div>

          {/* Password */}
          <div className="form-group">
            <label htmlFor="password">
              Password
            </label>

            <div className="input-wrapper">
              <LockKeyhole size={18} />

              <input
                id="password"
                type="password"
                placeholder="Create a password"
                value={password}
                onChange={(e) =>
                  setPassword(e.target.value)
                }
                required
              />
            </div>

            {/* Password Requirements */}
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

          {/* Confirm Password */}
          <div className="form-group">
            <label htmlFor="confirmPassword">
              Confirm password
            </label>

            <div className="input-wrapper">
              <LockKeyhole size={18} />

              <input
                id="confirmPassword"
                type="password"
                placeholder="Re-enter your password"
                value={confirmPassword}
                onChange={(e) =>
                  setConfirmPassword(
                    e.target.value
                  )
                }
                required
              />
            </div>

            {/* Password Match Message */}
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

          {/* Create Account */}
          <button
            type="submit"
            className="login-button"
            disabled={loading}
          >
            <span>
              {loading
                ? "Creating account..."
                : "Create account"}
            </span>
          </button>

        </form>

        {/* Login */}
        <div className="login-divider">
          <span>ALREADY A MEMBER?</span>
        </div>

        <Link
          to="/login"
          className="register-button"
        >
          Back to sign in
        </Link>

        <p className="security-note">
          Secure registration · Your information
          remains protected
        </p>

      </section>
    </main>
  );
}

export default Register;