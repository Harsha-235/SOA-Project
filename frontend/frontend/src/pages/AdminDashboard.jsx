import { useEffect, useState } from "react";
import {
  LayoutDashboard,
  Users,
  BookOpen,
  ClipboardList,
  IndianRupee,
  LogOut,
  CheckCircle2,
  XCircle,
  Clock3,
  RefreshCw,
  Search,
  UserCheck,
  UserX,
  ShieldCheck,
  PackageCheck,
  Pencil,
  Trash2,
  Plus,
  BarChart3,
  Activity,
  TrendingUp,
  PieChart as PieChartIcon,
  Download,
UserCircle,
} from "lucide-react";
import { useNavigate } from "react-router-dom";

import api from "../services/api";
import fineService from "../services/fineService";
import { jsPDF } from "jspdf";
import {
  ResponsiveContainer,
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from "recharts";

const CHART_COLORS = {
  blue: "#2563eb",
  cyan: "#06b6d4",
  green: "#22c55e",
  orange: "#f97316",
  purple: "#8b5cf6",
  pink: "#ec4899",
  yellow: "#eab308",
  red: "#ef4444",
  grid: "#dbe3ef",
};

function AdminDashboard() {
  const navigate = useNavigate();

  const [activeSection, setActiveSection] =
    useState("overview");

  const [users, setUsers] = useState([]);
  const [books, setBooks] = useState([]);
  const [borrowRequests, setBorrowRequests] =
    useState([]);
  const [pendingPayments, setPendingPayments] =
    useState([]);

  const [deletionRequests, setDeletionRequests] =
    useState([]);

  const [allBorrows, setAllBorrows] = useState([]);
  const [allFines, setAllFines] = useState([]);
  const [analyticsRange, setAnalyticsRange] = useState("all");

  const [loading, setLoading] = useState(false);
  const [processingId, setProcessingId] =
    useState(null);

  const [userSearch, setUserSearch] =
    useState("");

  const [bookSearch, setBookSearch] =
    useState("");

  const [showBookForm, setShowBookForm] =
    useState(false);

  const [editingBookId, setEditingBookId] =
    useState(null);

  const [bookForm, setBookForm] = useState({
    title: "",
    author: "",
    description: "",
    category: "",
    totalCopies: "",
    finePerDay: "",
    borrowingPeriodDays: "",
  });

  const [bookSubmitting, setBookSubmitting] =
    useState(false);

  /* ==========================================
     LOAD USERS
  ========================================== */

  const loadUsers = async () => {
    try {
      const response = await api.get(
        "/auth/admin/users"
      );

      setUsers(
        Array.isArray(response.data)
          ? response.data
          : []
      );
    } catch (error) {
      console.error(
        "Failed to load users:",
        error
      );
    }
  };

  /* ==========================================
     LOAD BOOKS
  ========================================== */

  const loadBooks = async () => {
    try {
      const response = await api.get("/books");

      setBooks(
        Array.isArray(response.data)
          ? response.data
          : []
      );
    } catch (error) {
      console.error(
        "Failed to load books:",
        error
      );
    }
  };

  /* ==========================================
     LOAD BORROW REQUESTS
  ========================================== */

  const loadBorrowRequests = async () => {
    try {
      const response = await api.get(
        "/borrows/admin/pending"
      );

      setBorrowRequests(
        Array.isArray(response.data)
          ? response.data
          : []
      );
    } catch (error) {
      console.error(
        "Failed to load borrow requests:",
        error
      );
    }
  };

  /* ==========================================
     LOAD PENDING PAYMENTS
  ========================================== */

  const loadPendingPayments = async () => {
    try {
      /*
       * The existing Fine Service currently exposes
       * user-based fine retrieval.
       *
       * For the moment we load the known test user's
       * fines and filter pending payments.
       *
       * This will be replaced by a dedicated
       * /fines/admin/pending endpoint once added.
       */

      const response =
        await fineService.getUserFines(24);

      const pending =
        Array.isArray(response)
          ? response.filter(
              (fine) =>
                fine.paymentStatus ===
                "PENDING_APPROVAL"
            )
          : [];

      setPendingPayments(pending);
    } catch (error) {
      console.error(
        "Failed to load payments:",
        error
      );
    }
  };

  /* ==========================================
     LOAD ACCOUNT DELETION REQUESTS
  ========================================== */

  const loadDeletionRequests = async () => {
    try {
      const response = await api.get(
        "/auth/admin/users/deletion-requests"
      );

      setDeletionRequests(
        Array.isArray(response.data)
          ? response.data
          : []
      );
    } catch (error) {
      console.error(
        "Failed to load account deletion requests:",
        error
      );
      setDeletionRequests([]);
    }
  };

  /* ==========================================
     LOAD ANALYTICS DATA
  ========================================== */

  const loadAnalyticsData = async () => {
    try {
      // Admin analytics must use the admin endpoint instead of
      // /borrows/user/{userId}, which is restricted to the logged-in user.
      const borrowResponse = await api.get("/borrows/admin/all");

      const allBorrowRecords = Array.isArray(borrowResponse.data)
        ? borrowResponse.data
        : [];

      const fineResults = await Promise.all(
        users.map(async (user) => {
          try {
            const response = await fineService.getUserFines(user.userId);
            return Array.isArray(response) ? response : [];
          } catch (error) {
            console.error(
              `Failed to load fines for user ${user.userId}:`,
              error
            );
            return [];
          }
        })
      );

      const borrowMap = new Map();
      allBorrowRecords.forEach((borrow) => {
        if (borrow?.borrowId != null) {
          borrowMap.set(borrow.borrowId, borrow);
        }
      });

      const fineMap = new Map();
      fineResults.flat().forEach((fine) => {
        if (fine?.fineId != null) {
          fineMap.set(fine.fineId, fine);
        }
      });

      setAllBorrows(Array.from(borrowMap.values()));
      setAllFines(Array.from(fineMap.values()));
    } catch (error) {
      console.error("Failed to load analytics data:", error);
      setAllBorrows([]);
      setAllFines([]);
    }
  };

  /* ==========================================
     LOAD EVERYTHING
  ========================================== */

  const loadDashboard = async () => {
    try {
      setLoading(true);

      const [userResponse] = await Promise.all([
        api.get("/auth/admin/users"),
        loadBooks(),
        loadBorrowRequests(),
        loadPendingPayments(),
        loadDeletionRequests(),
      ]);

      const loadedUsers = Array.isArray(userResponse.data)
        ? userResponse.data
        : [];

      setUsers(loadedUsers);

      // Admin analytics: load every borrowing record through the
      // dedicated admin endpoint. Do not call /borrows/user/{userId}
      // for other users because that endpoint is user-restricted.
      const borrowResponse = await api.get("/borrows/admin/all");

      const allBorrowRecords = Array.isArray(borrowResponse.data)
        ? borrowResponse.data
        : [];

      const fineResults = await Promise.all(
        loadedUsers.map(async (user) => {
          try {
            const response = await fineService.getUserFines(user.userId);
            return Array.isArray(response) ? response : [];
          } catch (error) {
            console.error(
              `Failed to load fines for user ${user.userId}:`,
              error
            );
            return [];
          }
        })
      );

      const borrowMap = new Map();
      allBorrowRecords.forEach((borrow) => {
        if (borrow?.borrowId != null) {
          borrowMap.set(borrow.borrowId, borrow);
        }
      });

      const fineMap = new Map();
      fineResults.flat().forEach((fine) => {
        if (fine?.fineId != null) {
          fineMap.set(fine.fineId, fine);
        }
      });

      setAllBorrows(Array.from(borrowMap.values()));
      setAllFines(Array.from(fineMap.values()));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
  }, []);

  /* ==========================================
     APPROVE PAYMENT
  ========================================== */

  const handleApprovePayment = async (
    fineId
  ) => {
    try {
      setProcessingId(fineId);

      await fineService.approvePayment(
        fineId
      );

      alert(
        "Payment approved successfully. Fine is now marked as PAID."
      );

      await loadPendingPayments();
    } catch (error) {
      console.error(
        "Payment approval failed:",
        error
      );

      alert(
        error.response?.data?.message ||
          "Unable to approve payment."
      );
    } finally {
      setProcessingId(null);
    }
  };

  /* ==========================================
     REJECT PAYMENT
  ========================================== */

  const handleRejectPayment = async (
    fineId
  ) => {
    try {
      setProcessingId(fineId);

      await fineService.rejectPayment(
        fineId
      );

      alert(
        "Payment rejected successfully."
      );

      await loadPendingPayments();
    } catch (error) {
      console.error(
        "Payment rejection failed:",
        error
      );

      alert(
        error.response?.data?.message ||
          "Unable to reject payment."
      );
    } finally {
      setProcessingId(null);
    }
  };

  /* ==========================================
     REJECT ACCOUNT DELETION
  ========================================== */

  const handleRejectDeletion = async (userId) => {
    if (
      !window.confirm(
        "Reject this account deletion request?"
      )
    ) {
      return;
    }

    try {
      setProcessingId(userId);

      await api.put(
        `/auth/admin/users/${userId}/delete-request/reject`
      );

      alert(
        "Account deletion request rejected successfully."
      );

      await loadDeletionRequests();
      await loadUsers();
    } catch (error) {
      console.error(
        "Deletion rejection failed:",
        error
      );

      alert(
        error.response?.data?.message ||
          "Unable to reject account deletion request."
      );
    } finally {
      setProcessingId(null);
    }
  };

  /* ==========================================
     APPROVE ACCOUNT DELETION
  ========================================== */

  const handleApproveDeletion = async (user) => {
    if (user.role === "ADMIN") {
      alert("The Admin account cannot be deleted.");
      return;
    }

    if (
      !window.confirm(
        `Permanently delete \"${user.name || "this user"}\"?\n\n` +
          "This will permanently remove the user's account, borrow history and fine history.\n\n" +
          "This action cannot be undone."
      )
    ) {
      return;
    }

    try {
      setProcessingId(user.userId);

      await api.put(
        `/auth/admin/users/${user.userId}/delete-request/approve`
      );

      alert(
        "User account permanently deleted successfully."
      );

      await loadDeletionRequests();
      await loadUsers();
    } catch (error) {
      console.error(
        "Deletion approval failed:",
        error
      );

      alert(
        error.response?.data?.message ||
          error.response?.data ||
          "Unable to permanently delete the user."
      );
    } finally {
      setProcessingId(null);
    }
  };

  /* ==========================================
     APPROVE BORROW
  ========================================== */

  const handleApproveBorrow = async (
    borrowId
  ) => {
    const days = window.prompt(
      "Enter borrowing period in days:",
      "7"
    );

    if (!days) return;

    const borrowingDays =
      Number(days);

    if (
      !Number.isInteger(
        borrowingDays
      ) ||
      borrowingDays < 1 ||
      borrowingDays > 60
    ) {
      alert(
        "Borrowing period must be between 1 and 60 days."
      );
      return;
    }

    try {
      setProcessingId(borrowId);

      await api.put(
        `/borrows/${borrowId}/approve`,
        {
          borrowingDays,
        }
      );

      alert(
        "Borrow request approved successfully."
      );

      await loadBorrowRequests();
    } catch (error) {
      console.error(
        "Borrow approval failed:",
        error
      );

      alert(
        error.response?.data?.message ||
          "Unable to approve borrow request."
      );
    } finally {
      setProcessingId(null);
    }
  };

  /* ==========================================
     REJECT BORROW
  ========================================== */

  const handleRejectBorrow = async (
    borrowId
  ) => {
    if (
      !window.confirm(
        "Reject this borrow request?"
      )
    ) {
      return;
    }

    try {
      setProcessingId(borrowId);

      await api.put(
        `/borrows/${borrowId}/reject`
      );

      alert(
        "Borrow request rejected successfully."
      );

      await loadBorrowRequests();
    } catch (error) {
      console.error(
        "Borrow rejection failed:",
        error
      );

      alert(
        error.response?.data?.message ||
          "Unable to reject borrow request."
      );
    } finally {
      setProcessingId(null);
    }
  };

  /* ==========================================
     USER MANAGEMENT
  ========================================== */

  const handleSuspendUser = async (
    userId
  ) => {
    if (
      !window.confirm(
        "Suspend this user account?"
      )
    ) {
      return;
    }

    try {
      setProcessingId(userId);

      await api.put(
        `/auth/admin/users/${userId}/suspend`
      );

      alert(
        "User account suspended successfully."
      );

      await loadUsers();
    } catch (error) {
      console.error(
        "Suspend failed:",
        error
      );

      alert(
        error.response?.data?.message ||
          "Unable to suspend user."
      );
    } finally {
      setProcessingId(null);
    }
  };

  const handleReactivateUser = async (
    userId
  ) => {
    try {
      setProcessingId(userId);

      await api.put(
        `/auth/admin/users/${userId}/reactivate`
      );

      alert(
        "User account reactivated successfully."
      );

      await loadUsers();
    } catch (error) {
      console.error(
        "Reactivate failed:",
        error
      );

      alert(
        error.response?.data?.message ||
          "Unable to reactivate user."
      );
    } finally {
      setProcessingId(null);
    }
  };

  const handleChangeUserName = async (user) => {
    const newName = window.prompt(
      `Enter a new name for ${user.name || "this user"}:`,
      user.name || ""
    );

    if (newName === null) return;

    const name = newName.trim();

    if (!name) {
      alert("Name is required.");
      return;
    }

    if (name.length < 2) {
      alert("Name must contain at least 2 characters.");
      return;
    }

    if (name === String(user.name || "").trim()) {
      alert("New name must be different from the current name.");
      return;
    }

    try {
      setProcessingId(user.userId);

      await api.put(`/auth/users/${user.userId}/profile`, {
        name,
      });

      alert("User name changed successfully.");
      await loadUsers();
    } catch (error) {
      console.error("Name change failed:", error);
      alert(
        error.response?.data?.message ||
          error.response?.data ||
          "Unable to change user name."
      );
    } finally {
      setProcessingId(null);
    }
  };

  const handleDeleteUser = async (user) => {
    if (user.role === "ADMIN") {
      alert("The Admin account cannot be deleted.");
      return;
    }

    const confirmed = window.confirm(
      `Permanently delete "${user.name || "this user"}"?\n\n` +
        "This will permanently remove the user's account, borrow history and fine history.\n\n" +
        "This action cannot be undone."
    );

    if (!confirmed) return;

    try {
      setProcessingId(user.userId);

      await api.delete(
        `/auth/admin/users/${user.userId}`
      );

      alert("User account permanently deleted successfully.");

      await loadUsers();
      await loadDeletionRequests();
    } catch (error) {
      console.error("Permanent deletion failed:", error);
      alert(
        error.response?.data?.message ||
          error.response?.data ||
          "Unable to permanently delete user."
      );
    } finally {
      setProcessingId(null);
    }
  };

  /* ==========================================
     LOGOUT
  ========================================== */

  const handleLogout = () => {
    localStorage.removeItem(
      "archivalia_token"
    );

    navigate("/login");
  };

  /* ==========================================
     FILTER USERS
  ========================================== */

  const filteredUsers =
    users.filter((user) => {

      const query =
        userSearch
          .trim()
          .toLowerCase();

      if (!query) return true;

      return (
        String(
          user.userId || ""
        )
          .toLowerCase()
          .includes(query) ||

        String(
          user.name || ""
        )
          .toLowerCase()
          .includes(query) ||

        String(
          user.email || ""
        )
          .toLowerCase()
          .includes(query) ||

        String(
          user.mobileNumber || ""
        )
          .toLowerCase()
          .includes(query)
      );
    });

  /* ==========================================
     BOOK MANAGEMENT - CREATE BOOK
  ========================================== */

  const handleBookFormChange = (event) => {
    const { name, value } = event.target;

    setBookForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const resetBookForm = () => {
    setBookForm({
      title: "",
      author: "",
      description: "",
      category: "",
      totalCopies: "",
      finePerDay: "",
      borrowingPeriodDays: "",
    });
  };

  const handleCreateBook = async (event) => {
    event.preventDefault();

    const title = bookForm.title.trim();
    const author = bookForm.author.trim();
    const description = bookForm.description.trim();
    const category = bookForm.category.trim();
    const totalCopies = Number(bookForm.totalCopies);
    const finePerDay = Number(bookForm.finePerDay);
    const borrowingPeriodDays = Number(bookForm.borrowingPeriodDays);

    if (!title || !author) {
      alert("Title and author are required.");
      return;
    }

    if (!Number.isInteger(totalCopies) || totalCopies < 1) {
      alert("Total copies must be at least 1.");
      return;
    }

    if (!Number.isFinite(finePerDay) || finePerDay < 0) {
      alert("Fine per day must be 0 or greater.");
      return;
    }

    if (
      !Number.isInteger(borrowingPeriodDays) ||
      borrowingPeriodDays < 1 ||
      borrowingPeriodDays > 60
    ) {
      alert("Borrowing period must be between 1 and 60 days.");
      return;
    }

    try {
      setBookSubmitting(true);

      if (editingBookId !== null) {
        const existingBook = books.find(
          (book) => Number(book.bookId) === Number(editingBookId)
        );

        if (!existingBook) {
          alert("The selected book could not be found.");
          return;
        }

        const currentlyBorrowed = Math.max(
          Number(existingBook.totalCopies || 0) -
            Number(existingBook.availableCopies || 0),
          0
        );

        if (totalCopies < currentlyBorrowed) {
          alert(
            `Total copies cannot be less than the ${currentlyBorrowed} copy/copies currently borrowed.`
          );
          return;
        }

        const availableCopies = totalCopies - currentlyBorrowed;

        await api.put(`/books/${editingBookId}`, {
          title,
          author,
          description,
          category,
          totalCopies,
          availableCopies,
          finePerDay,
          borrowingPeriodDays,
        });

        alert("Book updated successfully.");
      } else {
        await api.post("/books", {
          title,
          author,
          description,
          category,
          totalCopies,
          availableCopies: totalCopies,
          finePerDay,
          borrowingPeriodDays,
        });

        alert("Book added successfully.");
      }

      resetBookForm();
      setEditingBookId(null);
      setShowBookForm(false);
      await loadBooks();
    } catch (error) {
      console.error(
        editingBookId !== null
          ? "Book update failed:"
          : "Book creation failed:",
        error
      );

      alert(
        error.response?.data?.message ||
          (editingBookId !== null
            ? "Unable to update the book. Please try again."
            : "Unable to add the book. Please try again.")
      );
    } finally {
      setBookSubmitting(false);
    }
  };

  const handleEditBook = (book) => {
    setEditingBookId(book.bookId);

    setBookForm({
      title: book.title || "",
      author: book.author || "",
      description: book.description || "",
      category: book.category || "",
      totalCopies: String(book.totalCopies ?? ""),
      finePerDay: String(book.finePerDay ?? ""),
      borrowingPeriodDays: String(book.borrowingPeriodDays ?? ""),
    });

    setShowBookForm(true);
  };

  /* ==========================================
     BOOK MANAGEMENT - ARCHIVE BOOK
  ========================================== */

  const handleDeleteBook = async (book) => {
    const confirmed = window.confirm(
      `Archive "${book.title}"?\n\nThe book will be removed from the active catalogue, but its borrowing and fine history will be preserved.`
    );

    if (!confirmed) {
      return;
    }

    try {
      setProcessingId(book.bookId);

      const response = await api.delete(
        `/books/${book.bookId}`
      );

      alert(
        response.data ||
          "Book archived successfully."
      );

      await loadBooks();
    } catch (error) {
      console.error(
        "Book archive failed:",
        error
      );

      alert(
        error.response?.data?.message ||
          error.response?.data ||
          "Unable to archive the book. Please try again."
      );
    } finally {
      setProcessingId(null);
    }
  };

  /* ==========================================
     FILTER BOOKS
  ========================================== */

  const filteredBooks =
    books.filter((book) => {

      const query =
        bookSearch
          .trim()
          .toLowerCase();

      if (!query) return true;

      return (
        String(
          book.bookId || ""
        )
          .toLowerCase()
          .includes(query) ||

        String(
          book.title || ""
        )
          .toLowerCase()
          .includes(query) ||

        String(
          book.author || ""
        )
          .toLowerCase()
          .includes(query) ||

        String(
          book.category || ""
        )
          .toLowerCase()
          .includes(query)
      );
    });

  /* ==========================================
     ANALYTICS
  ========================================== */

  const getDateValue = (item) =>
    item?.issueDate ||
    item?.returnDate ||
    item?.createdAt ||
    item?.paymentSubmittedAt ||
    item?.paidAt ||
    null;

  const rangeStart = (() => {
    if (analyticsRange === "all") return null;
    const days = analyticsRange === "7d" ? 7 : analyticsRange === "30d" ? 30 : 180;
    const date = new Date();
    date.setDate(date.getDate() - days);
    date.setHours(0, 0, 0, 0);
    return date;
  })();

  const inSelectedRange = (item) => {
    if (!rangeStart) return true;
    const value = getDateValue(item);
    if (!value) return false;
    return new Date(value) >= rangeStart;
  };

  const analyticsBorrows = allBorrows.filter(inSelectedRange);
  const analyticsFines = allFines.filter(inSelectedRange);

  /*
   * BORROW STATUS NORMALIZATION
   *
   * Different backend responses may expose the active-loan state as
   * status, borrowStatus, or borrowingStatus.  The analytics should
   * also remain correct when an active loan has no returnDate yet.
   */
  const getBorrowStatus = (borrow) =>
    String(
      borrow?.status ??
      borrow?.borrowStatus ??
      borrow?.borrowingStatus ??
      ""
    )
      .trim()
      .toUpperCase();

  const isActiveBorrow = (borrow) => {
    const status = getBorrowStatus(borrow);

    if (["BORROWED", "ACTIVE", "ISSUED"].includes(status)) {
      return !borrow?.returnDate;
    }

    /*
     * Fallback for an active loan whose API response does not expose
     * a standard status value: an issued borrow with no return date
     * is treated as currently borrowed.
     */
    if (
      !status &&
      borrow?.issueDate &&
      !borrow?.returnDate
    ) {
      return true;
    }

    return false;
  };

  const isReturnedBorrow = (borrow) => {
    const status = getBorrowStatus(borrow);

    return (
      status === "RETURNED" ||
      Boolean(borrow?.returnDate)
    );
  };

  const isOverdueBorrow = (borrow) => {
    if (!isActiveBorrow(borrow) || !borrow?.dueDate) {
      return false;
    }

    return new Date(`${borrow.dueDate}T23:59:59`) < new Date();
  };

  /*
   * Keep the pie-chart categories mutually exclusive:
   *   - Borrowed = active and not overdue
   *   - Overdue  = active and past due date
   *   - Returned = already returned
   */
  const borrowingActivity = [
    {
      name: "Borrowed",
      value: analyticsBorrows.filter(
        (borrow) =>
          isActiveBorrow(borrow) &&
          !isOverdueBorrow(borrow)
      ).length,
    },
    {
      name: "Returned",
      value: analyticsBorrows.filter(isReturnedBorrow).length,
    },
    {
      name: "Overdue",
      value: analyticsBorrows.filter(isOverdueBorrow).length,
    },
  ];

  /* ==========================================
     SCALABLE ADMIN ANALYTICS
     These charts aggregate records so they remain
     readable even with hundreds of users/books.
  ========================================== */

  const userStatusMap = {
    ACTIVE: 0,
    SUSPENDED: 0,
  };

  users.forEach((user) => {
    const status = String(user.accountStatus || "SUSPENDED").toUpperCase();

    if (status === "ACTIVE") {
      userStatusMap.ACTIVE += 1;
    } else if (status === "SUSPENDED") {
      userStatusMap.SUSPENDED += 1;
    }
  });

  const userAccountStatus = [
    { name: "Active", users: userStatusMap.ACTIVE },
    { name: "Suspended", users: userStatusMap.SUSPENDED },
  ];

  const categoryMap = {};

  books.forEach((book) => {
    const category = String(book.category || "Uncategorized").trim() || "Uncategorized";

    if (!categoryMap[category]) {
      categoryMap[category] = {
        total: 0,
        available: 0,
        borrowed: 0,
      };
    }

    const total = Number(book.totalCopies || 0);
    const available = Number(book.availableCopies || 0);
    const borrowed = Math.max(0, total - available);

    categoryMap[category].total += total;
    categoryMap[category].available += Math.min(available, total);
    categoryMap[category].borrowed += borrowed;
  });

  const collectionUtilization = Object.entries(categoryMap)
    .map(([name, value]) => ({
      name,
      available: value.available,
      borrowed: value.borrowed,
      total: value.total,
    }))
    .sort((a, b) => b.total - a.total);

  // Data used by the existing Catalogue Distribution chart.
  // Keep this separate from Collection Utilization so both charts
  // can use their own aggregation without undefined variables.
  const categoryAnalytics = Object.entries(categoryMap)
    .map(([name, value]) => ({
      name,
      value: value.total,
      borrowed: value.borrowed,
    }))
    .sort((a, b) => b.value - a.value);

  const topBorrowedBooks = books
    .map((book) => ({
      name: book.title
        ? String(book.title).trim()
        : `Book #${book.bookId}`,
      borrows: analyticsBorrows.filter(
        (borrow) => Number(borrow.bookId) === Number(book.bookId)
      ).length,
    }))
    .filter((book) => book.borrows > 0)
    .sort((a, b) => b.borrows - a.borrows)
    .slice(0, 5);

  const buildTrend = () => {
    const now = new Date();
    const points = analyticsRange === "7d"
      ? 7
      : analyticsRange === "30d"
        ? 30
        : analyticsRange === "6m"
          ? 6
          : 6;

    if (analyticsRange === "7d" || analyticsRange === "30d") {
      return Array.from({ length: points }, (_, index) => {
        const date = new Date(now);
        date.setDate(now.getDate() - (points - 1 - index));
        const key = date.toISOString().slice(0, 10);
        return {
          name: date.toLocaleDateString("en-IN", { day: "2-digit", month: "short" }),
          borrowed: analyticsBorrows.filter((b) => String(b.issueDate || "").slice(0, 10) === key).length,
          returned: analyticsBorrows.filter((b) => String(b.returnDate || "").slice(0, 10) === key).length,
        };
      });
    }

    return Array.from({ length: points }, (_, index) => {
      const date = new Date(now.getFullYear(), now.getMonth() - (points - 1 - index), 1);
      const month = date.getMonth();
      const year = date.getFullYear();
      return {
        name: date.toLocaleDateString("en-IN", { month: "short", year: "2-digit" }),
        borrowed: analyticsBorrows.filter((b) => {
          const d = b.issueDate ? new Date(b.issueDate) : null;
          return d && d.getMonth() === month && d.getFullYear() === year;
        }).length,
        returned: analyticsBorrows.filter((b) => {
          const d = b.returnDate ? new Date(b.returnDate) : null;
          return d && d.getMonth() === month && d.getFullYear() === year;
        }).length,
      };
    });
  };

  const borrowingTrend = buildTrend();

  const fineAnalytics = [
    {
      name: "Unpaid",
      value: analyticsFines
        .filter((f) => f.status === "UNPAID")
        .reduce((sum, f) => sum + Number(f.fineAmount || 0), 0),
    },
    {
      name: "Paid",
      value: analyticsFines
        .filter((f) => f.status === "PAID")
        .reduce((sum, f) => sum + Number(f.fineAmount || 0), 0),
    },
    {
      name: "Waived",
      value: analyticsFines
        .filter((f) => f.status === "WAIVED")
        .reduce((sum, f) => sum + Number(f.fineAmount || 0), 0),
    },
  ];

  const exportUsersPDF = () => {
    const doc = new jsPDF({ orientation: "landscape" });
    doc.setFontSize(18);
    doc.text("Archivalia - Registered Users", 14, 16);
    doc.setFontSize(9);
    doc.text(`Generated: ${new Date().toLocaleString("en-IN")}`, 14, 23);

    let y = 34;
    doc.setFont("helvetica", "bold");
    doc.text("ID", 14, y);
    doc.text("Name", 30, y);
    doc.text("Email", 85, y);
    doc.text("Mobile", 190, y);
    doc.text("Role", 235, y);
    doc.text("Status", 265, y);
    y += 7;
    doc.setFont("helvetica", "normal");

    users.forEach((user) => {
      if (y > 190) {
        doc.addPage();
        y = 18;
      }
      doc.text(`#${user.userId}`, 14, y);
      doc.text(String(user.name || "-").slice(0, 25), 30, y);
      doc.text(String(user.email || "-").slice(0, 42), 85, y);
      doc.text(String(user.mobileNumber || "-"), 190, y);
      doc.text(String(user.role || "-"), 235, y);
      doc.text(String(user.accountStatus || "-"), 265, y);
      y += 6;
    });

    doc.save("archivalia-users.pdf");
  };

  const exportBooksPDF = () => {
    const doc = new jsPDF({ orientation: "landscape" });
    doc.setFontSize(18);
    doc.text("Archivalia - Library Catalogue", 14, 16);
    doc.setFontSize(9);
    doc.text(`Generated: ${new Date().toLocaleString("en-IN")}`, 14, 23);

    let y = 34;
    doc.setFont("helvetica", "bold");
    doc.text("ID", 14, y);
    doc.text("Title", 30, y);
    doc.text("Author", 105, y);
    doc.text("Category", 165, y);
    doc.text("Total", 215, y);
    doc.text("Available", 235, y);
    doc.text("Fine/Day", 260, y);
    doc.text("Days", 285, y);
    y += 7;
    doc.setFont("helvetica", "normal");

    books.forEach((book) => {
      if (y > 190) {
        doc.addPage();
        y = 18;
      }
      doc.text(`#${book.bookId}`, 14, y);
      doc.text(String(book.title || "-").slice(0, 36), 30, y);
      doc.text(String(book.author || "-").slice(0, 28), 105, y);
      doc.text(String(book.category || "-").slice(0, 20), 165, y);
      doc.text(String(book.totalCopies ?? "-"), 215, y);
      doc.text(String(book.availableCopies ?? "-"), 235, y);
      doc.text(`Rs.${book.finePerDay ?? "-"}`, 260, y);
      doc.text(String(book.borrowingPeriodDays ?? "-"), 285, y);
      y += 6;
    });

    doc.save("archivalia-books.pdf");
  };

  const chartTooltipStyle = {
    backgroundColor: "#ffffff",
    border: "1px solid #d9dde2",
    borderRadius: "6px",
    fontSize: "11px",
  };

  return (
    <div className="admin-page">

      {/* ======================================
          HEADER
      ====================================== */}

      <header className="admin-header">

        <div className="admin-brand">

          <div className="admin-brand-mark">
            <BookOpen size={18} />
          </div>

          <div>
            <strong>
              Archivalia
            </strong>

            <span>
              ADMINISTRATION
            </span>
          </div>

        </div>

        <div
  style={{
    display: "flex",
    gap: "8px",
    alignItems: "center",
  }}
>
  <button
    className="admin-logout"
    onClick={() => navigate("/profile")}
  >
    <UserCircle size={14} />
    Profile
  </button>

  <button
    className="admin-logout"
    onClick={handleLogout}
  >
    <LogOut size={14} />
    Sign out
  </button>
</div>

      </header>

      {/* ======================================
          BODY
      ====================================== */}

      <main className="admin-container">

        {/* WELCOME */}

        <section className="admin-welcome">

          <div>

            <p className="admin-eyebrow">
              ADMINISTRATION
            </p>

            <h1>
              Administration Dashboard
            </h1>

            <p>
              Manage users, books,
              borrow requests and
              payment verification.
            </p>

          </div>

          <button
            className="admin-refresh"
            onClick={loadDashboard}
            disabled={loading}
          >
            <RefreshCw
              size={13}
              className={
                loading
                  ? "refresh-spinning"
                  : ""
              }
            />

            Refresh
          </button>

        </section>

        {/* ======================================
            NAVIGATION
        ====================================== */}

        <nav className="admin-navigation">

          <button
            className={
              activeSection ===
              "overview"
                ? "admin-nav-active"
                : ""
            }
            onClick={() =>
              setActiveSection(
                "overview"
              )
            }
          >
            <LayoutDashboard size={14} />
            Overview
          </button>

          <button
            className={
              activeSection ===
              "users"
                ? "admin-nav-active"
                : ""
            }
            onClick={() =>
              setActiveSection("users")
            }
          >
            <Users size={14} />
            Users
          </button>

          <button
            className={
              activeSection ===
              "books"
                ? "admin-nav-active"
                : ""
            }
            onClick={() =>
              setActiveSection("books")
            }
          >
            <BookOpen size={14} />
            Books
          </button>

          <button
            className={
              activeSection ===
              "borrowings"
                ? "admin-nav-active"
                : ""
            }
            onClick={() =>
              setActiveSection(
                "borrowings"
              )
            }
          >
            <ClipboardList size={14} />
            Borrow Requests
          </button>

          <button
            className={
              activeSection ===
              "payments"
                ? "admin-nav-active"
                : ""
            }
            onClick={() =>
              setActiveSection(
                "payments"
              )
            }
          >
            <IndianRupee size={14} />
            Payments
          </button>

          <button
            className={
              activeSection ===
              "deletions"
                ? "admin-nav-active"
                : ""
            }
            onClick={() =>
              setActiveSection(
                "deletions"
              )
            }
          >
            <Trash2 size={14} />
            Account Deletions
            {deletionRequests.length > 0 && (
              <span
                style={{
                  marginLeft: "4px",
                  minWidth: "18px",
                  height: "18px",
                  padding: "0 5px",
                  borderRadius: "999px",
                  display: "inline-flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: "10px",
                  fontWeight: 700,
                  background: "#f3f4f6",
                  color: "#111827",
                }}
              >
                {deletionRequests.length}
              </span>
            )}
          </button>

        </nav>

        {/* ======================================
            OVERVIEW
        ====================================== */}

        {activeSection ===
          "overview" && (

          <>
            <section className="admin-stats">

              <div className="admin-stat-card">

                <div className="admin-stat-icon">
                  <Users size={16} />
                </div>

                <div>
                  <span>
                    Total Users
                  </span>

                  <strong>
                    {users.length}
                  </strong>
                </div>

              </div>

              <div className="admin-stat-card">

                <div className="admin-stat-icon">
                  <BookOpen size={16} />
                </div>

                <div>
                  <span>
                    Total Books
                  </span>

                  <strong>
                    {books.length}
                  </strong>
                </div>

              </div>

              <div className="admin-stat-card">

                <div className="admin-stat-icon">
                  <ClipboardList size={16} />
                </div>

                <div>
                  <span>
                    Borrow Requests
                  </span>

                  <strong>
                    {borrowRequests.length}
                  </strong>
                </div>

              </div>

              <div className="admin-stat-card">

                <div className="admin-stat-icon">
                  <IndianRupee size={16} />
                </div>

                <div>
                  <span>
                    Pending Payments
                  </span>

                  <strong>
                    {pendingPayments.length}
                  </strong>
                </div>

              </div>

              <div className="admin-stat-card">

                <div className="admin-stat-icon">
                  <ShieldCheck size={16} />
                </div>

                <div>
                  <span>
                    Active Users
                  </span>

                  <strong>
                    {
                      users.filter(
                        (user) =>
                          user.accountStatus ===
                          "ACTIVE"
                      ).length
                    }
                  </strong>
                </div>

              </div>

            </section>

            {/* ADVANCED ANALYTICS */}

            <section className="admin-card">
              <div className="admin-card-header">
                <div>
                  <p className="admin-eyebrow">LIBRARY ANALYTICS</p>
                  <h2>Advanced Analytics</h2>
                  <span style={{ fontSize: "11px", color: "#68727d" }}>
                    Operational insights from borrowing, catalogue and fine activity.
                  </span>
                </div>

                <div style={{ display: "flex", gap: "6px", flexWrap: "wrap" }}>
                  {[
                    ["7d", "7 Days"],
                    ["30d", "30 Days"],
                    ["6m", "6 Months"],
                    ["all", "All Time"],
                  ].map(([value, label]) => (
                    <button
                      key={value}
                      onClick={() => setAnalyticsRange(value)}
                      className={analyticsRange === value ? "admin-primary-button" : "table-action edit"}
                      style={{ minWidth: "66px" }}
                    >
                      {label}
                    </button>
                  ))}
                </div>
              </div>

              <div style={{
                display: "grid",
                gridTemplateColumns: "repeat(4, minmax(0, 1fr))",
                gap: "10px",
                marginBottom: "14px"
              }}>
                {[
                  ["Tracked Borrows", analyticsBorrows.length, ClipboardList],
                  ["Currently Borrowed", borrowingActivity[0].value, Activity],
                  ["Overdue", borrowingActivity[2].value, Clock3],
                  ["Fine Value", `₹${analyticsFines.reduce((s, f) => s + Number(f.fineAmount || 0), 0).toFixed(2)}`, IndianRupee],
                ].map(([label, value, Icon]) => (
                  <div key={label} className="admin-stat-card">
                    <div className="admin-stat-icon"><Icon size={16} /></div>
                    <div>
                      <span>{label}</span>
                      <strong>{value}</strong>
                    </div>
                  </div>
                ))}
              </div>

              <div style={{
                display: "grid",
                gridTemplateColumns: "repeat(2, minmax(0, 1fr))",
                gap: "14px"
              }}>
                <div className="admin-card" style={{ margin: 0, padding: "14px" }}>
                  <div className="admin-card-header">
                    <div>
                      <p className="admin-eyebrow">BORROWING ACTIVITY</p>
                      <h2 style={{ fontSize: "18px" }}>Borrowed vs Returned vs Overdue</h2>
                    </div>
                  </div>
                  <div style={{ width: "100%", height: 270 }}>
                    <ResponsiveContainer>
                      <PieChart>
                        <Pie data={borrowingActivity} dataKey="value" nameKey="name" cx="50%" cy="48%" outerRadius={85} innerRadius={48} paddingAngle={3}>
                          {borrowingActivity.map((entry, index) => <Cell key={entry.name} fill={[CHART_COLORS.blue, CHART_COLORS.green, CHART_COLORS.red][index]} />)}
                        </Pie>
                        <Tooltip contentStyle={chartTooltipStyle} />
                        <Legend wrapperStyle={{ fontSize: "11px" }} />
                      </PieChart>
                    </ResponsiveContainer>
                  </div>
                </div>

                <div className="admin-card" style={{ margin: 0, padding: "14px" }}>
                  <div className="admin-card-header">
                    <div>
                      <p className="admin-eyebrow">BORROWING TREND</p>
                      <h2 style={{ fontSize: "18px" }}>Circulation Over Time</h2>
                    </div>
                  </div>
                  <div style={{ width: "100%", height: 270 }}>
                    <ResponsiveContainer>
                      <LineChart data={borrowingTrend}>
                        <CartesianGrid strokeDasharray="3 3" stroke={CHART_COLORS.grid} />
                        <XAxis dataKey="name" tick={{ fontSize: 10 }} />
                        <YAxis allowDecimals={false} tick={{ fontSize: 10 }} />
                        <Tooltip contentStyle={chartTooltipStyle} />
                        <Legend wrapperStyle={{ fontSize: "11px" }} />
                        <Line type="monotone" dataKey="borrowed" stroke={CHART_COLORS.blue} strokeWidth={2} dot={{ r: 2 }} />
                        <Line type="monotone" dataKey="returned" stroke={CHART_COLORS.green} strokeWidth={2} dot={{ r: 2 }} />
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                </div>

                <div className="admin-card" style={{ margin: 0, padding: "14px" }}>
                  <div className="admin-card-header">
                    <div>
                      <p className="admin-eyebrow">USER MANAGEMENT</p>
                      <h2 style={{ fontSize: "18px" }}>User Account Status</h2>
                    </div>
                  </div>
                  <div style={{ width: "100%", height: 270 }}>
                    <ResponsiveContainer>
                      <BarChart
                        data={userAccountStatus}
                        layout="vertical"
                        margin={{ top: 8, right: 18, left: 12, bottom: 8 }}
                      >
                        <CartesianGrid strokeDasharray="3 3" stroke={CHART_COLORS.grid} />
                        <XAxis
                          type="number"
                          allowDecimals={false}
                          tick={{ fontSize: 10 }}
                        />
                        <YAxis
                          type="category"
                          dataKey="name"
                          width={82}
                          tick={{ fontSize: 10 }}
                        />
                        <Tooltip
                          contentStyle={chartTooltipStyle}
                          formatter={(value) => [value, "Users"]}
                        />
                        <Bar
                          dataKey="users"
                          name="Users"
                          fill={CHART_COLORS.blue}
                          radius={[0, 3, 3, 0]}
                          barSize={24}
                        />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </div>

                <div className="admin-card" style={{ margin: 0, padding: "14px" }}>
                  <div className="admin-card-header">
                    <div>
                      <p className="admin-eyebrow">COLLECTION UTILIZATION</p>
                      <h2 style={{ fontSize: "18px" }}>Collection Utilization by Category</h2>
                    </div>
                  </div>
                  <div style={{ width: "100%", height: 270 }}>
                    <ResponsiveContainer>
                      <BarChart
                        data={collectionUtilization}
                        layout="vertical"
                        margin={{ top: 8, right: 18, left: 12, bottom: 8 }}
                      >
                        <CartesianGrid strokeDasharray="3 3" stroke={CHART_COLORS.grid} />
                        <XAxis
                          type="number"
                          allowDecimals={false}
                          tick={{ fontSize: 10 }}
                        />
                        <YAxis
                          type="category"
                          dataKey="name"
                          width={92}
                          tick={{ fontSize: 10 }}
                        />
                        <Tooltip contentStyle={chartTooltipStyle} />
                        <Legend wrapperStyle={{ fontSize: "11px" }} />
                        <Bar
                          dataKey="available"
                          name="Available"
                          stackId="copies"
                          fill={CHART_COLORS.orange}
                          radius={[0, 0, 0, 0]}
                        />
                        <Bar
                          dataKey="borrowed"
                          name="Borrowed"
                          stackId="copies"
                          fill={CHART_COLORS.blue}
                          radius={[0, 3, 3, 0]}
                        />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </div>

                <div className="admin-card" style={{ margin: 0, padding: "14px" }}>
                  <div className="admin-card-header">
                    <div>
                      <p className="admin-eyebrow">CATEGORY ANALYTICS</p>
                      <h2 style={{ fontSize: "18px" }}>Catalogue Distribution</h2>
                    </div>
                  </div>
                  <div style={{ width: "100%", height: 270 }}>
                    <ResponsiveContainer>
                      <BarChart data={categoryAnalytics}>
                        <CartesianGrid strokeDasharray="3 3" stroke={CHART_COLORS.grid} />
                        <XAxis dataKey="name" tick={{ fontSize: 10 }} />
                        <YAxis allowDecimals={false} tick={{ fontSize: 10 }} />
                        <Tooltip contentStyle={chartTooltipStyle} />
                        <Legend wrapperStyle={{ fontSize: "11px" }} />
                        <Bar dataKey="value" name="Copies" fill={CHART_COLORS.blue} />
                        <Bar dataKey="borrowed" name="Borrowed" fill={CHART_COLORS.orange} />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </div>

                <div className="admin-card" style={{ margin: 0, padding: "14px" }}>
                  <div className="admin-card-header">
                    <div>
                      <p className="admin-eyebrow">FINE ANALYTICS</p>
                      <h2 style={{ fontSize: "18px" }}>Fine Value by Status</h2>
                    </div>
                  </div>
                  <div style={{ width: "100%", height: 270 }}>
                    <ResponsiveContainer>
                      <PieChart>
                        <Pie data={fineAnalytics} dataKey="value" nameKey="name" cx="50%" cy="48%" outerRadius={85} innerRadius={48} paddingAngle={3}>
                          {fineAnalytics.map((entry, index) => <Cell key={entry.name} fill={[CHART_COLORS.red, CHART_COLORS.blue, CHART_COLORS.orange][index]} />)}
                        </Pie>
                        <Tooltip formatter={(value) => `₹${Number(value).toFixed(2)}`} contentStyle={chartTooltipStyle} />
                        <Legend wrapperStyle={{ fontSize: "11px" }} />
                      </PieChart>
                    </ResponsiveContainer>
                  </div>
                </div>

                <div className="admin-card" style={{ margin: 0, padding: "14px", gridColumn: "1 / -1" }}>
                  <div className="admin-card-header">
                    <div>
                      <p className="admin-eyebrow">TOP BORROWED BOOKS</p>
                      <h2 style={{ fontSize: "18px" }}>Top 5 Most Borrowed Books</h2>
                    </div>
                  </div>
                  <div style={{ width: "100%", height: 280 }}>
                    <ResponsiveContainer>
                      <BarChart
                        data={topBorrowedBooks}
                        layout="vertical"
                        margin={{ top: 8, right: 22, left: 12, bottom: 8 }}
                      >
                        <CartesianGrid strokeDasharray="3 3" stroke={CHART_COLORS.grid} />
                        <XAxis
                          type="number"
                          allowDecimals={false}
                          tick={{ fontSize: 10 }}
                        />
                        <YAxis
                          type="category"
                          dataKey="name"
                          width={150}
                          tick={{ fontSize: 10 }}
                        />
                        <Tooltip
                          contentStyle={chartTooltipStyle}
                          formatter={(value) => [value, "Borrow Count"]}
                        />
                        <Bar
                          dataKey="borrows"
                          name="Borrow Count"
                          fill={CHART_COLORS.blue}
                          radius={[0, 3, 3, 0]}
                          barSize={28}
                        />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </div>
              </div>
            </section>
          </>
        )}

        {/* ======================================
            USERS
        ====================================== */}

        {activeSection ===
          "users" && (

          <section className="admin-card">

            <div className="admin-card-header">

              <div>

                <p className="admin-eyebrow">
                  USER MANAGEMENT
                </p>

                <h2>
                  Registered Users
                </h2>

              </div>

              <div className="admin-header-tools">
                <div className="admin-search">

                  <Search size={13} />

                  <input
                    type="text"
                    placeholder="Search users..."
                    value={userSearch}
                    onChange={(event) =>
                      setUserSearch(
                        event.target.value
                      )
                    }
                  />

                </div>

                <button
                  className="admin-primary-button"
                  onClick={exportUsersPDF}
                  disabled={users.length === 0}
                >
                  <Download size={13} />
                  Download PDF
                </button>
              </div>

            </div>

            {loading ? (

              <div className="admin-empty">
                Loading users...
              </div>

            ) : (

              <div className="admin-table-wrapper">

                <table className="admin-table">

                  <thead>

                    <tr>
                      <th>ID</th>
                      <th>User</th>
                      <th>Email</th>
                      <th>Mobile</th>
                      <th>Role</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>

                  </thead>

                  <tbody>

                    {filteredUsers.map(
                      (user) => {

                        const isAdmin =
                          user.role ===
                          "ADMIN";

                        const active =
                          user.accountStatus ===
                          "ACTIVE";

                        return (
                          <tr
                            key={
                              user.userId
                            }
                          >

                            <td>
                              #{user.userId}
                            </td>

                            <td>
                              <strong>
                                {user.name ||
                                  "-"}
                              </strong>
                            </td>

                            <td>
                              {user.email}
                            </td>

                            <td>
                              {user.mobileNumber ||
                                "-"}
                            </td>

                            <td>
                              <span className="admin-role">
                                {user.role}
                              </span>
                            </td>

                            <td>
                              <span
                                className={
                                  active
                                    ? "admin-status-active"
                                    : "admin-status-inactive"
                                }
                              >
                                {user.accountStatus ||
                                  "UNKNOWN"}
                              </span>

                              {user.deletionRequested && (
                                <span
                                  style={{
                                    display: "inline-block",
                                    marginLeft: "6px",
                                    fontSize: "10px",
                                    padding: "2px 6px",
                                    borderRadius: "10px",
                                    background: "#fff4e5",
                                    color: "#8a5a00",
                                    border: "1px solid #e6c98a",
                                  }}
                                >
                                  Deletion Requested
                                </span>
                              )}
                            </td>

                            <td>

                              {!isAdmin && (
                                <div className="admin-action-group">

                                  {active ? (

                                    <button
                                      className="table-action suspend"
                                      onClick={() =>
                                        handleSuspendUser(
                                          user.userId
                                        )
                                      }
                                      disabled={
                                        processingId ===
                                        user.userId
                                      }
                                    >
                                      <UserX
                                        size={12}
                                      />
                                      Suspend
                                    </button>

                                  ) : (

                                    <button
                                      className="table-action reactivate"
                                      onClick={() =>
                                        handleReactivateUser(
                                          user.userId
                                        )
                                      }
                                      disabled={
                                        processingId ===
                                        user.userId
                                      }
                                    >
                                      <UserCheck
                                        size={12}
                                      />
                                      Reactivate
                                    </button>

                                  )}

                                  <button
                                    className="table-action edit"
                                    onClick={() =>
                                      handleChangeUserName(user)
                                    }
                                    disabled={
                                      processingId ===
                                      user.userId
                                    }
                                  >
                                    <Pencil size={12} />
                                    Change Name
                                  </button>

                                  <button
                                    className="table-action deactivate"
                                    onClick={() =>
                                      handleDeleteUser(user)
                                    }
                                    disabled={
                                      processingId ===
                                      user.userId
                                    }
                                    title="Permanently delete user account"
                                  >
                                    <Trash2 size={12} />
                                    Delete
                                  </button>

                                </div>
                              )}

                              {isAdmin && (
                                <span className="admin-protected">
                                  <ShieldCheck
                                    size={12}
                                  />
                                  Protected
                                </span>
                              )}

                            </td>

                          </tr>
                        );
                      }
                    )}

                  </tbody>

                </table>

              </div>
            )}

          </section>
        )}

        {/* ======================================
            BOOKS
        ====================================== */}

        {activeSection ===
          "books" && (

          <section className="admin-card">

            <div className="admin-card-header">

              <div>

                <p className="admin-eyebrow">
                  BOOK MANAGEMENT
                </p>

                <h2>
                  Library Catalogue
                </h2>

              </div>

              <div className="admin-header-tools">

                <div className="admin-search">

                  <Search size={13} />

                  <input
                    type="text"
                    placeholder="Search books..."
                    value={bookSearch}
                    onChange={(event) =>
                      setBookSearch(
                        event.target.value
                      )
                    }
                  />

                </div>

                <button
                  className="admin-primary-button"
                  onClick={() => {
                    resetBookForm();
                    setEditingBookId(null);
                    setShowBookForm(true);
                  }}
                >
                  <Plus size={13} />
                  Add Book
                </button>

                <button
                  className="admin-primary-button"
                  onClick={exportBooksPDF}
                  disabled={books.length === 0}
                >
                  <Download size={13} />
                  Download PDF
                </button>

              </div>

            </div>

            {showBookForm && (
              <div
                style={{
                  marginTop: "16px",
                  border: "1px solid #d9dde2",
                  borderRadius: "8px",
                  backgroundColor: "#ffffff",
                  padding: "18px",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "flex-start",
                    gap: "16px",
                    marginBottom: "16px",
                  }}
                >
                  <div>
                    <p className="admin-eyebrow">
                      BOOK MANAGEMENT
                    </p>
                    <h3
                      style={{
                        margin: "2px 0 4px",
                        fontFamily: "inherit",
                        fontSize: "18px",
                        fontWeight: 600,
                      }}
                    >
                      {editingBookId !== null
                        ? "Edit Book"
                        : "Add New Book"}
                    </h3>
                    <span
                      style={{
                        fontSize: "11px",
                        color: "#68727d",
                      }}
                    >
                      {editingBookId !== null
                        ? "Update the selected resource in the Archivalia library catalogue."
                        : "Add a resource to the Archivalia library catalogue."}
                    </span>
                  </div>

                  <button
                    type="button"
                    className="table-action edit"
                    onClick={() => {
                      resetBookForm();
                      setEditingBookId(null);
                      setShowBookForm(false);
                    }}
                  >
                    <XCircle size={12} />
                    Cancel
                  </button>
                </div>

                <form onSubmit={handleCreateBook}>
                  <div
                    style={{
                      display: "grid",
                      gridTemplateColumns: "repeat(2, minmax(0, 1fr))",
                      gap: "14px",
                    }}
                  >
                    <label style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                      <span style={{ fontSize: "11px", fontWeight: 600 }}>Title *</span>
                      <input
                        name="title"
                        value={bookForm.title}
                        onChange={handleBookFormChange}
                        placeholder="Enter book title"
                        required
                        style={{ padding: "10px 11px", border: "1px solid #d9dde2", borderRadius: "5px", fontSize: "12px" }}
                      />
                    </label>

                    <label style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                      <span style={{ fontSize: "11px", fontWeight: 600 }}>Author *</span>
                      <input
                        name="author"
                        value={bookForm.author}
                        onChange={handleBookFormChange}
                        placeholder="Enter author name"
                        required
                        style={{ padding: "10px 11px", border: "1px solid #d9dde2", borderRadius: "5px", fontSize: "12px" }}
                      />
                    </label>

                    <label style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                      <span style={{ fontSize: "11px", fontWeight: 600 }}>Category</span>
                      <input
                        name="category"
                        value={bookForm.category}
                        onChange={handleBookFormChange}
                        placeholder="e.g. Computer Science"
                        style={{ padding: "10px 11px", border: "1px solid #d9dde2", borderRadius: "5px", fontSize: "12px" }}
                      />
                    </label>

                    <label style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                      <span style={{ fontSize: "11px", fontWeight: 600 }}>Total Copies *</span>
                      <input
                        type="number"
                        name="totalCopies"
                        value={bookForm.totalCopies}
                        onChange={handleBookFormChange}
                        min="1"
                        step="1"
                        placeholder="e.g. 5"
                        required
                        style={{ padding: "10px 11px", border: "1px solid #d9dde2", borderRadius: "5px", fontSize: "12px" }}
                      />
                    </label>

                    <label style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                      <span style={{ fontSize: "11px", fontWeight: 600 }}>Fine Per Day (₹) *</span>
                      <input
                        type="number"
                        name="finePerDay"
                        value={bookForm.finePerDay}
                        onChange={handleBookFormChange}
                        min="0"
                        step="0.01"
                        placeholder="e.g. 10"
                        required
                        style={{ padding: "10px 11px", border: "1px solid #d9dde2", borderRadius: "5px", fontSize: "12px" }}
                      />
                    </label>

                    <label style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                      <span style={{ fontSize: "11px", fontWeight: 600 }}>Borrowing Period (Days) *</span>
                      <input
                        type="number"
                        name="borrowingPeriodDays"
                        value={bookForm.borrowingPeriodDays}
                        onChange={handleBookFormChange}
                        min="1"
                        max="60"
                        step="1"
                        placeholder="e.g. 14"
                        required
                        style={{ padding: "10px 11px", border: "1px solid #d9dde2", borderRadius: "5px", fontSize: "12px" }}
                      />
                    </label>

                    <label style={{ display: "flex", flexDirection: "column", gap: "6px", gridColumn: "1 / -1" }}>
                      <span style={{ fontSize: "11px", fontWeight: 600 }}>Description</span>
                      <textarea
                        name="description"
                        value={bookForm.description}
                        onChange={handleBookFormChange}
                        placeholder="Enter a short description of the book"
                        rows={3}
                        style={{ padding: "10px 11px", border: "1px solid #d9dde2", borderRadius: "5px", fontSize: "12px", resize: "vertical", fontFamily: "inherit" }}
                      />
                    </label>
                  </div>

                  <div
                    style={{
                      display: "flex",
                      justifyContent: "flex-end",
                      gap: "8px",
                      marginTop: "16px",
                    }}
                  >
                    <button
                      type="button"
                      className="table-action edit"
                      onClick={() => {
                        resetBookForm();
                        setShowBookForm(false);
                      }}
                      disabled={bookSubmitting}
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="admin-primary-button"
                      disabled={bookSubmitting}
                    >
                      {editingBookId !== null ? (
                        <Pencil size={13} />
                      ) : (
                        <Plus size={13} />
                      )}
                      {bookSubmitting
                        ? editingBookId !== null
                          ? "Saving..."
                          : "Adding..."
                        : editingBookId !== null
                          ? "Save Changes"
                          : "Add Book"}
                    </button>
                  </div>
                </form>
              </div>
            )}

            <div className="admin-table-wrapper">

              <table className="admin-table">

                <thead>

                  <tr>
                    <th>ID</th>
                    <th>Book</th>
                    <th>Author</th>
                    <th>Category</th>
                    <th>Total</th>
                    <th>Available</th>
                    <th>Fine/Day</th>
                    <th>Period</th>
                    <th>Actions</th>
                  </tr>

                </thead>

                <tbody>

                  {filteredBooks.map(
                    (book) => (

                      <tr
                        key={
                          book.bookId
                        }
                      >

                        <td>
                          #{book.bookId}
                        </td>

                        <td>
                          <strong>
                            {book.title}
                          </strong>
                        </td>

                        <td>
                          {book.author}
                        </td>

                        <td>
                          {book.category ||
                            "-"}
                        </td>

                        <td>
                          {book.totalCopies}
                        </td>

                        <td>
                          {book.availableCopies}
                        </td>

                        <td>
                          ₹{book.finePerDay}
                        </td>

                        <td>
                          {book.borrowingPeriodDays}
                          {" days"}
                        </td>

                        <td>

                          <div className="admin-action-group">

                            <button
                              className="table-action edit"
                              onClick={() =>
                                handleEditBook(book)
                              }
                              disabled={bookSubmitting}
                            >
                              <Pencil
                                size={12}
                              />
                              Edit
                            </button>

                            <button
                              className="table-action deactivate"
                              onClick={() =>
                                handleDeleteBook(book)
                              }
                              disabled={
                                processingId ===
                                book.bookId
                              }
                            >
                              <Trash2
                                size={12}
                              />
                              {processingId ===
                              book.bookId
                                ? "Archiving..."
                                : "Delete"}
                            </button>

                          </div>

                        </td>

                      </tr>
                    )
                  )}

                </tbody>

              </table>

            </div>

          </section>
        )}

        {/* ======================================
            BORROW REQUESTS
        ====================================== */}

        {activeSection ===
          "borrowings" && (

          <section className="admin-card">

            <div className="admin-card-header">

              <div>

                <p className="admin-eyebrow">
                  BORROW MANAGEMENT
                </p>

                <h2>
                  Pending Borrow Requests
                </h2>

              </div>

              <span className="pending-count">
                <Clock3 size={12} />
                {borrowRequests.length}
                {" pending"}
              </span>

            </div>

            {borrowRequests.length ===
            0 ? (

              <div className="admin-empty">

                <CheckCircle2
                  size={22}
                />

                <span>
                  No pending borrow
                  requests.
                </span>

              </div>

            ) : (

              <div className="payment-approval-list">

                {borrowRequests.map(
                  (request) => (

                    <div
                      className="payment-approval-item"
                      key={
                        request.borrowId
                      }
                    >

                      <div className="payment-approval-icon">
                        <ClipboardList
                          size={17}
                        />
                      </div>

                      <div className="payment-approval-details">

                        <h3>
                          Borrow #
                          {request.borrowId}
                        </h3>

                        <span>
                          User #
                          {request.userId}
                          {" · "}
                          {request.userName ||
                            "-"}
                        </span>

                        <span>
                          Book #
                          {request.bookId}
                          {" · "}
                          {request.bookTitle ||
                            "-"}
                        </span>

                        <span>
                          {request.userEmail ||
                            ""}
                        </span>

                      </div>

                      <div className="payment-approval-status">
                        <span>
                          PENDING
                        </span>
                      </div>

                      <div className="payment-approval-actions">

                        <button
                          className="reject-payment-button"
                          onClick={() =>
                            handleRejectBorrow(
                              request.borrowId
                            )
                          }
                          disabled={
                            processingId ===
                            request.borrowId
                          }
                        >
                          <XCircle
                            size={13}
                          />
                          Reject
                        </button>

                        <button
                          className="approve-payment-button"
                          onClick={() =>
                            handleApproveBorrow(
                              request.borrowId
                            )
                          }
                          disabled={
                            processingId ===
                            request.borrowId
                          }
                        >
                          <CheckCircle2
                            size={13}
                          />

                          {processingId ===
                          request.borrowId
                            ? "Processing..."
                            : "Approve"}
                        </button>

                      </div>

                    </div>
                  )
                )}

              </div>
            )}

          </section>
        )}

        {/* ======================================
            PAYMENTS
        ====================================== */}

        {activeSection ===
          "payments" && (

          <section className="admin-card">

            <div className="admin-card-header">

              <div>

                <p className="admin-eyebrow">
                  PAYMENT VERIFICATION
                </p>

                <h2>
                  Pending Payment Approvals
                </h2>

              </div>

              <span className="pending-count">
                <Clock3 size={12} />
                {pendingPayments.length}
                {" pending"}
              </span>

            </div>

            {pendingPayments.length ===
            0 ? (

              <div className="admin-empty">

                <CheckCircle2
                  size={22}
                />

                <span>
                  No payment requests
                  are waiting for
                  approval.
                </span>

              </div>

            ) : (

              <div className="payment-approval-list">

                {pendingPayments.map(
                  (fine) => (

                    <div
                      className="payment-approval-item"
                      key={fine.fineId}
                    >

                      <div className="payment-approval-icon">
                        <IndianRupee
                          size={17}
                        />
                      </div>

                      <div className="payment-approval-details">

                        <h3>
                          Fine #
                          {fine.fineId}
                        </h3>

                        <span>
                          User #
                          {fine.userId}
                          {" · "}
                          Book #
                          {fine.bookId}
                          {" · "}
                          Borrow #
                          {fine.borrowId}
                        </span>

                        <span>
                          Submitted:
                          {" "}
                          {fine.paymentSubmittedAt
                            ? new Date(
                                fine.paymentSubmittedAt
                              ).toLocaleString()
                            : "-"}
                        </span>

                      </div>

                      <div className="payment-approval-amount">

                        <span>
                          Amount
                        </span>

                        <strong>
                          ₹
                          {fine.fineAmount}
                        </strong>

                      </div>

                      <div className="payment-approval-status">
                        <span>
                          PENDING APPROVAL
                        </span>
                      </div>

                      <div className="payment-approval-actions">

                        <button
                          className="reject-payment-button"
                          onClick={() =>
                            handleRejectPayment(
                              fine.fineId
                            )
                          }
                          disabled={
                            processingId ===
                            fine.fineId
                          }
                        >
                          <XCircle
                            size={13}
                          />
                          Reject
                        </button>

                        <button
                          className="approve-payment-button"
                          onClick={() =>
                            handleApprovePayment(
                              fine.fineId
                            )
                          }
                          disabled={
                            processingId ===
                            fine.fineId
                          }
                        >
                          <CheckCircle2
                            size={13}
                          />

                          {processingId ===
                          fine.fineId
                            ? "Processing..."
                            : "Approve"}
                        </button>

                      </div>

                    </div>
                  )
                )}

              </div>
            )}

          </section>
        )}

        {/* ======================================
            ACCOUNT DELETIONS
        ====================================== */}

        {activeSection ===
          "deletions" && (

          <section className="admin-card">

            <div className="admin-card-header">

              <div>
                <p className="admin-eyebrow">
                  ACCOUNT MANAGEMENT
                </p>

                <h2>
                  Account Deletion Requests
                </h2>
              </div>

              <span className="pending-count">
                <Clock3 size={12} />
                {deletionRequests.length}
                {" pending"}
              </span>

            </div>

            {deletionRequests.length === 0 ? (

              <div className="admin-empty">
                <CheckCircle2 size={22} />
                <span>
                  No account deletion requests are waiting for approval.
                </span>
              </div>

            ) : (

              <div className="payment-approval-list">

                {deletionRequests.map((user) => (

                  <div
                    className="payment-approval-item"
                    key={user.userId}
                  >

                    <div className="payment-approval-icon">
                      <Trash2 size={17} />
                    </div>

                    <div className="payment-approval-details">
                      <h3>
                        {user.name || "User"}
                      </h3>

                      <span>
                        User #{user.userId}
                        {" · "}
                        {user.email || "-"}
                      </span>

                      <span>
                        Requested: {user.deletionRequestedAt
                          ? new Date(
                              user.deletionRequestedAt
                            ).toLocaleString()
                          : "-"}
                      </span>
                    </div>

                    <div className="payment-approval-status">
                      <span>
                        PENDING APPROVAL
                      </span>
                    </div>

                    <div className="payment-approval-actions">

                      <button
                        className="reject-payment-button"
                        onClick={() =>
                          handleRejectDeletion(
                            user.userId
                          )
                        }
                        disabled={
                          processingId === user.userId
                        }
                      >
                        <XCircle size={13} />
                        Reject
                      </button>

                      <button
                        className="approve-payment-button"
                        onClick={() =>
                          handleApproveDeletion(user)
                        }
                        disabled={
                          processingId === user.userId
                        }
                      >
                        <CheckCircle2 size={13} />
                        {processingId === user.userId
                          ? "Processing..."
                          : "Approve & Delete"}
                      </button>

                    </div>

                  </div>
                ))}

              </div>
            )}

          </section>
        )}

      </main>

    </div>
  );
}

export default AdminDashboard;