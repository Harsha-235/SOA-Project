export const getTokenPayload = () => {
  const token = localStorage.getItem("archivalia_token");

  if (!token) {
    return null;
  }

  try {
    return JSON.parse(
      atob(
        token
          .split(".")[1]
          .replace(/-/g, "+")
          .replace(/_/g, "/")
      )
    );
  } catch (error) {
    console.error("Unable to decode JWT:", error);
    return null;
  }
};

export const getCurrentUserId = () => {
  const payload = getTokenPayload();

  return payload?.userId || payload?.user_id || payload?.sub || null;
};