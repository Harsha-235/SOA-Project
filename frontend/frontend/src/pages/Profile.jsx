import { useEffect, useState } from "react";
import {
  ArrowLeft,
  BookOpen,
  User,
  Mail,
  Phone,
  ShieldCheck,
  Pencil,
  Trash2,
  X,
  Clock3,
} from "lucide-react";

import api from "../services/api";
import userService from "../services/userService";
import { getCurrentUserId } from "../utils/auth";

function Profile() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const [editingName, setEditingName] = useState(false);
  const [name, setName] = useState("");
  const [savingName, setSavingName] = useState(false);

  const [deletionRequested, setDeletionRequested] = useState(false);
  const [deletionRequestedAt, setDeletionRequestedAt] = useState("");

  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [requestingDeletion, setRequestingDeletion] = useState(false);

  const userId = getCurrentUserId();

  const isAdmin =
    String(user?.role || "").toUpperCase() === "ADMIN";

  // ==========================================
  // LOAD PROFILE
  // ==========================================

  const loadProfile = async () => {
    try {
      setLoading(true);

      if (!userId) {
        window.location.href = "/login";
        return;
      }

      const [userData, deletionData] = await Promise.all([
        userService.getUserById(userId),
        api.get(`/auth/users/${userId}/delete-request`),
      ]);

      setUser(userData);
      setName(userData?.name || "");

      setDeletionRequested(
        Boolean(deletionData?.data?.deletionRequested)
      );

      setDeletionRequestedAt(
        deletionData?.data?.deletionRequestedAt || ""
      );
    } catch (error) {
      console.error("Failed to load profile:", error);

      const message =
        error.response?.data?.message ||
        "Unable to load your profile.";

      alert(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProfile();
  }, []);

  // ==========================================
  // UPDATE NAME
  // ==========================================

  const handleSaveName = async () => {
    const trimmedName = name.trim();

    if (!trimmedName) {
      alert("Name cannot be empty.");
      return;
    }

    if (trimmedName === user?.name) {
      setEditingName(false);
      return;
    }

    try {
      setSavingName(true);

      const response = await api.put(
        `/auth/users/${userId}/profile`,
        {
          name: trimmedName,
        }
      );

      setUser(response.data);
      setName(response.data?.name || trimmedName);
      setEditingName(false);

      alert("Profile name updated successfully.");
    } catch (error) {
      console.error("Profile update failed:", error);

      const message =
        error.response?.data?.message ||
        "Unable to update your name.";

      alert(message);
    } finally {
      setSavingName(false);
    }
  };

  // ==========================================
  // REQUEST ACCOUNT DELETION
  // ==========================================

  const handleRequestDeletion = async () => {
    if (isAdmin) {
      return;
    }

    try {
      setRequestingDeletion(true);

      await api.post(
        `/auth/users/${userId}/delete-request`
      );

      setDeletionRequested(true);
      setDeletionRequestedAt(new Date().toISOString());
      setShowDeleteConfirm(false);

      alert(
        "Account deletion request submitted successfully. Waiting for administrator approval."
      );
    } catch (error) {
      console.error(
        "Account deletion request failed:",
        error
      );

      const message =
        error.response?.data?.message ||
        "Unable to submit account deletion request.";

      alert(message);
    } finally {
      setRequestingDeletion(false);
    }
  };

  // ==========================================
  // LOGOUT
  // ==========================================

  const handleLogout = () => {
    localStorage.removeItem("archivalia_token");
    window.location.href = "/login";
  };

  // ==========================================
  // LOADING
  // ==========================================

  if (loading) {
    return (
      <div
        style={{
          minHeight: "100vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          background: "#f8fafc",
          color: "#374151",
          fontSize: "14px",
        }}
      >
        Loading profile...
      </div>
    );
  }

  if (!user) {
    return null;
  }

  return (
    <div
      style={{
        minHeight: "100vh",
        background: "#f8fafc",
        color: "#111827",
      }}
    >
      {/* ==========================================
          HEADER
      ========================================== */}

      <header
        style={{
          height: "76px",
          background: "#ffffff",
          borderBottom: "1px solid #e5e7eb",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          padding: "0 7%",
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "12px",
          }}
        >
          <div
            style={{
              width: "40px",
              height: "40px",
              border: "1px solid #9ca3af",
              borderRadius: "9px",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            <BookOpen size={20} />
          </div>

          <div>
            <strong
              style={{
                display: "block",
                fontFamily: "Georgia, serif",
                fontSize: "20px",
              }}
            >
              Archivalia
            </strong>

            <span
              style={{
                display: "block",
                fontSize: "9px",
                letterSpacing: "2px",
                color: "#6b7280",
              }}
            >
              DIGITAL E-LIBRARY
            </span>
          </div>
        </div>

        <button
          onClick={handleLogout}
          style={{
            border: "1px solid #d1d5db",
            background: "#ffffff",
            borderRadius: "7px",
            padding: "9px 14px",
            cursor: "pointer",
            fontSize: "12px",
          }}
        >
          Sign out
        </button>
      </header>

      {/* ==========================================
          MAIN
      ========================================== */}

      <main
        style={{
          maxWidth: "900px",
          margin: "0 auto",
          padding: "42px 24px 70px",
        }}
      >
        {/* BACK BUTTON */}

        <button
         onClick={() => {
  window.location.href = isAdmin
    ? "/admin"
    : "/dashboard";
}}
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "7px",
            border: "none",
            background: "transparent",
            cursor: "pointer",
            color: "#4b5563",
            fontSize: "13px",
            padding: 0,
            marginBottom: "28px",
          }}
        >
          <ArrowLeft size={15} />
          Back to Dashboard
        </button>

        {/* TITLE */}

        <div style={{ marginBottom: "28px" }}>
          <p
            style={{
              margin: "0 0 7px",
              fontSize: "10px",
              letterSpacing: "2px",
              fontWeight: 700,
              color: "#6b7280",
            }}
          >
            ACCOUNT SETTINGS
          </p>

          <h1
            style={{
              margin: 0,
              fontFamily: "Georgia, serif",
              fontSize: "34px",
              fontWeight: 500,
            }}
          >
            My Profile
          </h1>

          <p
            style={{
              marginTop: "9px",
              color: "#6b7280",
              fontSize: "13px",
            }}
          >
            Manage your personal information and account settings.
          </p>
        </div>

        {/* ==========================================
            PERSONAL INFORMATION
        ========================================== */}

        <section
          style={{
            background: "#ffffff",
            border: "1px solid #e5e7eb",
            borderRadius: "10px",
            padding: "26px",
            marginBottom: "20px",
          }}
        >
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "10px",
              marginBottom: "24px",
            }}
          >
            <User size={18} />

            <div>
              <h2
                style={{
                  margin: 0,
                  fontSize: "17px",
                }}
              >
                Personal Information
              </h2>

              <p
                style={{
                  margin: "4px 0 0",
                  color: "#6b7280",
                  fontSize: "12px",
                }}
              >
                Your registered account information.
              </p>
            </div>
          </div>

          {/* NAME */}

          <div
            style={{
              padding: "17px 0",
              borderTop: "1px solid #edf0f3",
            }}
          >
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                gap: "15px",
              }}
            >
              <div style={{ flex: 1 }}>
                <label
                  style={{
                    display: "block",
                    fontSize: "11px",
                    color: "#6b7280",
                    marginBottom: "6px",
                  }}
                >
                  NAME
                </label>

                {editingName ? (
                  <input
                    value={name}
                    onChange={(event) =>
                      setName(event.target.value)
                    }
                    autoFocus
                    style={{
                      width: "100%",
                      maxWidth: "420px",
                      boxSizing: "border-box",
                      padding: "10px 12px",
                      border: "1px solid #9ca3af",
                      borderRadius: "7px",
                      outline: "none",
                      fontSize: "13px",
                    }}
                  />
                ) : (
                  <strong style={{ fontSize: "14px" }}>
                    {user.name}
                  </strong>
                )}
              </div>

              {!editingName ? (
                <button
                  onClick={() => setEditingName(true)}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "6px",
                    border: "1px solid #d1d5db",
                    background: "#ffffff",
                    borderRadius: "7px",
                    padding: "8px 12px",
                    cursor: "pointer",
                    fontSize: "12px",
                  }}
                >
                  <Pencil size={13} />
                  Edit
                </button>
              ) : (
                <div
                  style={{
                    display: "flex",
                    gap: "7px",
                  }}
                >
                  <button
                    onClick={() => {
                      setName(user.name);
                      setEditingName(false);
                    }}
                    disabled={savingName}
                    style={{
                      border: "1px solid #d1d5db",
                      background: "#ffffff",
                      borderRadius: "7px",
                      padding: "8px 12px",
                      cursor: "pointer",
                      fontSize: "12px",
                    }}
                  >
                    Cancel
                  </button>

                  <button
                    onClick={handleSaveName}
                    disabled={savingName}
                    style={{
                      border: "none",
                      background: "#111827",
                      color: "#ffffff",
                      borderRadius: "7px",
                      padding: "8px 12px",
                      cursor: "pointer",
                      fontSize: "12px",
                    }}
                  >
                    {savingName ? "Saving..." : "Save"}
                  </button>
                </div>
              )}
            </div>

            <p
              style={{
                margin: "8px 0 0",
                color: "#6b7280",
                fontSize: "11px",
              }}
            >
              {isAdmin
                ? "Administrators can change their name without a limit."
                : "Users can change their name a maximum of 2 times."}
            </p>
          </div>

          {/* EMAIL */}

          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "14px",
              padding: "17px 0",
              borderTop: "1px solid #edf0f3",
            }}
          >
            <Mail size={17} />

            <div>
              <span
                style={{
                  display: "block",
                  fontSize: "11px",
                  color: "#6b7280",
                  marginBottom: "5px",
                }}
              >
                EMAIL
              </span>

              <strong style={{ fontSize: "14px" }}>
                {user.email}
              </strong>
            </div>
          </div>

          {/* MOBILE */}

          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "14px",
              padding: "17px 0",
              borderTop: "1px solid #edf0f3",
            }}
          >
            <Phone size={17} />

            <div>
              <span
                style={{
                  display: "block",
                  fontSize: "11px",
                  color: "#6b7280",
                  marginBottom: "5px",
                }}
              >
                MOBILE NUMBER
              </span>

              <strong style={{ fontSize: "14px" }}>
                {user.mobileNumber || "-"}
              </strong>
            </div>
          </div>
          {/* ROLE */}

<div
  style={{
    display: "flex",
    alignItems: "center",
    gap: "14px",
    padding: "17px 0",
    borderTop: "1px solid #edf0f3",
  }}
>
  <ShieldCheck size={17} />

  <div>
    <span
      style={{
        display: "block",
        fontSize: "11px",
        color: "#6b7280",
        marginBottom: "5px",
      }}
    >
      ROLE
    </span>

    <strong
      style={{
        display: "inline-block",
        fontSize: "12px",
        padding: "5px 9px",
        borderRadius: "999px",
        background:
          isAdmin ? "#eef2ff" : "#f3f4f6",
        color:
          isAdmin ? "#3730a3" : "#374151",
      }}
    >
      {isAdmin ? "ADMIN" : "USER"}
    </strong>
  </div>
</div>
        </section>

        {/* ==========================================
            ACCOUNT STATUS
        ========================================== */}

        <section
          style={{
            background: "#ffffff",
            border: "1px solid #e5e7eb",
            borderRadius: "10px",
            padding: "26px",
            marginBottom: isAdmin ? "0" : "20px",
          }}
        >
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "10px",
              marginBottom: "20px",
            }}
          >
            <ShieldCheck size={18} />

            <h2
              style={{
                margin: 0,
                fontSize: "17px",
              }}
            >
              Account
            </h2>
          </div>

          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              padding: "16px",
              background: "#f8fafc",
              borderRadius: "8px",
            }}
          >
            <div>
              <span
                style={{
                  display: "block",
                  fontSize: "11px",
                  color: "#6b7280",
                  marginBottom: "5px",
                }}
              >
                ACCOUNT STATUS
              </span>

              <strong style={{ fontSize: "14px" }}>
                {user.accountStatus || "ACTIVE"}
              </strong>
            </div>

            <span
              style={{
                fontSize: "11px",
                fontWeight: 700,
                padding: "6px 10px",
                borderRadius: "999px",
                background: "#eef8f1",
                color: "#216e39",
              }}
            >
              {user.accountStatus || "ACTIVE"}
            </span>
          </div>
        </section>

        {/* ==========================================
            DELETE ACCOUNT
            USER ONLY
        ========================================== */}

        {!isAdmin && (
          <section
            style={{
              background: "#ffffff",
              border: "1px solid #e5e7eb",
              borderRadius: "10px",
              padding: "26px",
            }}
          >
            <div
              style={{
                display: "flex",
                alignItems: "flex-start",
                justifyContent: "space-between",
                gap: "25px",
              }}
            >
              <div>
                <h2
                  style={{
                    margin: "0 0 8px",
                    fontSize: "17px",
                  }}
                >
                  Delete My Account
                </h2>

                <p
                  style={{
                    margin: 0,
                    color: "#6b7280",
                    fontSize: "12px",
                    lineHeight: 1.6,
                    maxWidth: "570px",
                  }}
                >
                  Request permanent deletion of your Archivalia
                  account. An administrator must approve the
                  request before your account is permanently
                  removed.
                </p>

                <p
                  style={{
                    margin: "10px 0 0",
                    color: "#6b7280",
                    fontSize: "11px",
                  }}
                >
                  Permanent deletion also removes your historical
                  borrowing and fine records.
                </p>
              </div>

              {deletionRequested ? (
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "7px",
                    whiteSpace: "nowrap",
                    padding: "9px 13px",
                    borderRadius: "7px",
                    background: "#fff7ed",
                    color: "#9a3412",
                    fontSize: "12px",
                    fontWeight: 600,
                  }}
                >
                  <Clock3 size={14} />
                  Pending Approval
                </div>
              ) : (
                <button
                  onClick={() => setShowDeleteConfirm(true)}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "7px",
                    whiteSpace: "nowrap",
                    border: "1px solid #dc2626",
                    background: "#ffffff",
                    color: "#b91c1c",
                    borderRadius: "7px",
                    padding: "9px 13px",
                    cursor: "pointer",
                    fontSize: "12px",
                    fontWeight: 600,
                  }}
                >
                  <Trash2 size={14} />
                  Delete My Account
                </button>
              )}
            </div>

            {deletionRequestedAt && (
              <p
                style={{
                  margin: "18px 0 0",
                  paddingTop: "15px",
                  borderTop: "1px solid #edf0f3",
                  color: "#6b7280",
                  fontSize: "11px",
                }}
              >
                Request submitted on{" "}
                {new Date(deletionRequestedAt).toLocaleString()}
              </p>
            )}
          </section>
        )}
      </main>

      {/* ==========================================
          DELETE CONFIRMATION MODAL
          USER ONLY
      ========================================== */}

      {!isAdmin && showDeleteConfirm && (
        <div
          onClick={() => {
            if (!requestingDeletion) {
              setShowDeleteConfirm(false);
            }
          }}
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(15, 23, 42, 0.45)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: "20px",
            zIndex: 1000,
          }}
        >
          <div
            onClick={(event) => event.stopPropagation()}
            style={{
              width: "100%",
              maxWidth: "470px",
              background: "#ffffff",
              borderRadius: "12px",
              padding: "26px",
              boxShadow: "0 20px 50px rgba(0,0,0,0.15)",
            }}
          >
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "flex-start",
                gap: "15px",
              }}
            >
              <div>
                <p
                  style={{
                    margin: "0 0 6px",
                    fontSize: "10px",
                    letterSpacing: "2px",
                    fontWeight: 700,
                    color: "#b91c1c",
                  }}
                >
                  ACCOUNT DELETION
                </p>

                <h2
                  style={{
                    margin: 0,
                    fontFamily: "Georgia, serif",
                    fontSize: "23px",
                  }}
                >
                  Are you sure?
                </h2>
              </div>

              <button
                onClick={() => setShowDeleteConfirm(false)}
                disabled={requestingDeletion}
                style={{
                  border: "none",
                  background: "transparent",
                  cursor: "pointer",
                }}
              >
                <X size={18} />
              </button>
            </div>

            <div
              style={{
                marginTop: "20px",
                padding: "15px",
                background: "#fff7ed",
                border: "1px solid #fed7aa",
                borderRadius: "8px",
                color: "#7c2d12",
                fontSize: "12px",
                lineHeight: 1.6,
              }}
            >
              Your account will first be submitted for
              administrator approval. If approved, your account
              and historical borrow/fine records will be
              permanently deleted.
            </div>

            <div
              style={{
                display: "flex",
                justifyContent: "flex-end",
                gap: "9px",
                marginTop: "22px",
              }}
            >
              <button
                onClick={() => setShowDeleteConfirm(false)}
                disabled={requestingDeletion}
                style={{
                  border: "1px solid #d1d5db",
                  background: "#ffffff",
                  borderRadius: "7px",
                  padding: "9px 15px",
                  cursor: "pointer",
                  fontSize: "12px",
                }}
              >
                Cancel
              </button>

              <button
                onClick={handleRequestDeletion}
                disabled={requestingDeletion}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "7px",
                  border: "none",
                  background: "#b91c1c",
                  color: "#ffffff",
                  borderRadius: "7px",
                  padding: "9px 15px",
                  cursor: "pointer",
                  fontSize: "12px",
                  fontWeight: 600,
                }}
              >
                <Trash2 size={14} />

                {requestingDeletion
                  ? "Submitting..."
                  : "Request Deletion"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Profile;