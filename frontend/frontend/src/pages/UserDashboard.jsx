import { useEffect, useState } from "react";

import {

  BookOpen,
  Search,

  Clock3,

  AlertTriangle,

  CircleDollarSign,

  CheckCircle2,

  Download,

  RotateCcw,

  QrCode,

  X,

  CreditCard,
  UserCircle,

} from "lucide-react";

import { jsPDF } from "jspdf";



import borrowService from "../services/borrowService";

import userService from "../services/userService";

import fineService from "../services/fineService";
import api from "../services/api";

import { getCurrentUserId } from "../utils/auth";



function UserDashboard() {

  const [userName, setUserName] = useState("Member");

  const [borrows, setBorrows] = useState([]);

  const [fines, setFines] = useState([]);

  const [loading, setLoading] = useState(true);

  const [returningId, setReturningId] = useState(null);
  const [requestingBookId, setRequestingBookId] = useState(null);

  // Book catalogue
  const [books, setBooks] = useState([]);
  const [bookSearch, setBookSearch] = useState("");



  // Payment modal

  const [selectedFine, setSelectedFine] = useState(null);

  const [submittingPayment, setSubmittingPayment] = useState(false);



  const loadDashboard = async () => {

    try {

      setLoading(true);



      const userId = getCurrentUserId();



      if (!userId) {

        console.error("User ID not found in JWT.");

        return;

      }



      const [borrowData, userData, fineData] = await Promise.all([

        borrowService.getUserBorrows(userId),

        userService.getUserById(userId),

        fineService.getUserFines(userId),

      ]);



      setBorrows(Array.isArray(borrowData) ? borrowData : []);

      setFines(Array.isArray(fineData) ? fineData : []);



      if (userData?.name) {

        setUserName(userData.name);

      }

      try {
        const bookResponse = await api.get("/books");
        setBooks(
          Array.isArray(bookResponse.data)
            ? bookResponse.data.filter((book) => book.active !== false)
            : []
        );
      } catch (bookError) {
        console.error("Failed to load book catalogue:", bookError);
        setBooks([]);
      }

    } catch (error) {

      console.error("Failed to load dashboard:", error);

    } finally {

      setLoading(false);

    }

  };



  useEffect(() => {

    loadDashboard();

  }, []);



  // ==============================

  // DATE HELPERS

  // ==============================



  const today = new Date();

  today.setHours(0, 0, 0, 0);



  const getDaysDifference = (dateString) => {

    if (!dateString) return null;



    const date = new Date(`${dateString}T00:00:00`);

    date.setHours(0, 0, 0, 0);



    return Math.ceil(

      (date.getTime() - today.getTime()) / (1000 * 60 * 60 * 24)

    );

  };



  // ==============================

  // BORROW STATISTICS

  // ==============================



  const activeLoans = borrows.filter(

    (borrow) => borrow.status === "BORROWED"

  );



  const returnedBooks = borrows.filter(

    (borrow) => borrow.status === "RETURNED"

  );



  const overdueLoans = activeLoans.filter((borrow) => {

    const days = getDaysDifference(borrow.dueDate);

    return days !== null && days < 0;

  });



  const dueSoon = activeLoans.filter((borrow) => {

    const days = getDaysDifference(borrow.dueDate);

    return days !== null && days >= 0 && days <= 3;

  });



  // ==============================

  // FINE STATISTICS

  // ==============================



  const unpaidFines = fines.filter(

    (fine) => fine.status === "UNPAID"

  );



  const outstandingFines = unpaidFines.reduce(

    (total, fine) => total + Number(fine.fineAmount || 0),

    0

  );



  // ==============================

  // RETURN BOOK

  // ==============================



  const handleReturn = async (borrowId) => {

    try {

      setReturningId(borrowId);



      const response = await borrowService.returnBorrow(borrowId);



      console.log("Return response:", response);



      alert("Book returned successfully.");



      await loadDashboard();

    } catch (error) {

      console.error("Failed to return book:", error);



      const message =

        error.response?.data?.message ||

        "Unable to return the book.";



      alert(message);

    } finally {

      setReturningId(null);

    }

  };



  // ==============================

  // OPEN PAYMENT MODAL

  // ==============================



  const handlePayFine = (fine) => {

    setSelectedFine(fine);

  };



  // ==============================

  // SUBMIT PAYMENT

  // ==============================



  const handlePaymentSubmitted = async () => {

    if (!selectedFine) return;



    try {

      setSubmittingPayment(true);



      const response = await fineService.submitPayment(

        selectedFine.fineId

      );



      console.log("Payment submission response:", response);



      alert(

        "Payment submitted successfully. Waiting for admin approval."

      );



      setSelectedFine(null);



      await loadDashboard();

    } catch (error) {

      console.error("Payment submission failed:", error);



      const message =

        error.response?.data?.message ||

        "Unable to submit payment.";



      alert(message);

    } finally {

      setSubmittingPayment(false);

    }

  };



  // ==============================

  // DOWNLOAD ACTIVE LOANS PDF

  // ==============================



  const downloadPDF = () => {

    const doc = new jsPDF();



    doc.setFontSize(18);

    doc.text("Archivalia Digital E-Library", 20, 20);



    doc.setFontSize(12);

    doc.text(`Member: ${userName}`, 20, 30);

    doc.text("Current Loans", 20, 42);



    let y = 55;



    doc.setFontSize(10);



    doc.text("Borrow ID", 20, y);

    doc.text("Book ID", 55, y);

    doc.text("Issue Date", 90, y);

    doc.text("Due Date", 135, y);

    doc.text("Status", 175, y);



    y += 8;



    activeLoans.forEach((borrow) => {

      doc.text(String(borrow.borrowId), 20, y);

      doc.text(String(borrow.bookId), 55, y);

      doc.text(String(borrow.issueDate || "-"), 90, y);

      doc.text(String(borrow.dueDate || "-"), 135, y);

      doc.text(String(borrow.status || "-"), 175, y);



      y += 8;



      if (y > 275) {

        doc.addPage();

        y = 20;

      }

    });



    doc.save("archivalia-current-loans.pdf");

  };



  // ==============================
  // REQUEST BOOK
  // ==============================

  const getBookRequestStatus = (bookId) => {
    const activeBorrow = borrows.find(
      (borrow) =>
        Number(borrow.bookId) === Number(bookId) &&
        ["PENDING", "BORROWED"].includes(
          String(borrow.status || "").toUpperCase()
        )
    );

    return activeBorrow
      ? String(activeBorrow.status || "").toUpperCase()
      : null;
  };

  const handleRequestBook = async (book) => {
    if (Number(book.availableCopies) <= 0) {
      return;
    }

    const confirmed = window.confirm(
      `Request "${book.title}" for borrowing?\\n\\nYour request will be sent to the administrator for approval.`
    );

    if (!confirmed) {
      return;
    }

    try {
      setRequestingBookId(book.bookId);

      await api.post("/borrows/request", {
        bookId: book.bookId,
      });

      alert(
        "Borrow request submitted successfully. Waiting for admin approval."
      );

      await loadDashboard();
    } catch (error) {
      console.error("Book request failed:", error);

      const message =
        error.response?.data?.message ||
        error.response?.data ||
        "Unable to submit the borrow request.";

      alert(message);
    } finally {
      setRequestingBookId(null);
    }
  };

  // ==============================
  // BOOK CATALOGUE
  // ==============================

  const filteredBooks = books.filter((book) => {
    const query = bookSearch.trim().toLowerCase();

    if (!query) return true;

    return (
      String(book.title || "").toLowerCase().includes(query) ||
      String(book.author || "").toLowerCase().includes(query) ||
      String(book.category || "").toLowerCase().includes(query)
    );
  });

  // ==============================
  // LOGOUT
  // ==============================

  const handleLogout = () => {

    localStorage.removeItem("archivalia_token");

    window.location.href = "/login";

  };



  return (

    <div className="dashboard-page">



      {/* ==========================================

          HEADER

      ========================================== */}



      <header className="dashboard-header">

        <div className="dashboard-brand">

          <div className="dashboard-brand-mark">

            <BookOpen size={18} />

          </div>



          <div>

            <strong>Archivalia</strong>

            <span>DIGITAL E-LIBRARY</span>

          </div>

        </div>



        <div className="dashboard-header-actions">

          <button
            type="button"
            className="header-icon-button"
            onClick={() => {
              window.location.href = "/profile";
            }}
            title="Profile"
          >
            <UserCircle size={17} />
            <span>Profile</span>
          </button>



          <button

            className="logout-button"

            onClick={handleLogout}

          >

            Sign out

          </button>

        </div>

      </header>



      <main className="dashboard-container">



        {/* ==========================================

            WELCOME

        ========================================== */}



        <section className="dashboard-welcome">



          <div>

            <p className="section-eyebrow">

              MEMBER DASHBOARD

            </p>



            <h1>

              Welcome back, {userName}

            </h1>



            <p className="dashboard-description">

              Manage your borrowed resources, discover books,

              and keep track of your library activity.

            </p>

          </div>



          <button
            className="browse-button"
            onClick={() =>
              document.getElementById("book-catalogue")?.scrollIntoView({
                behavior: "smooth",
              })
            }
          >

            <BookOpen size={14} />

            Browse Library

          </button>



        </section>



        {/* ==========================================

            STATISTICS

        ========================================== */}



        
        {/* ==========================================
            BOOK CATALOGUE
        ========================================== */}

        <section
          id="book-catalogue"
          className="dashboard-card"
          style={{ marginBottom: "24px" }}
        >
          <div
            className="card-header"
            style={{ gap: "16px", flexWrap: "wrap" }}
          >
            <div>
              <p className="section-eyebrow">LIBRARY CATALOGUE</p>
              <h2>Browse available books</h2>
            </div>

            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "8px",
                border: "1px solid #d9dee7",
                borderRadius: "8px",
                padding: "8px 12px",
                minWidth: "260px",
                background: "#fff",
              }}
            >
              <Search size={15} />
              <input
                type="text"
                value={bookSearch}
                onChange={(event) => setBookSearch(event.target.value)}
                placeholder="Search title, author or category..."
                style={{
                  border: "none",
                  outline: "none",
                  width: "100%",
                  background: "transparent",
                  fontSize: "13px",
                }}
              />
            </div>
          </div>

          {loading ? (
            <div className="dashboard-empty">Loading book catalogue...</div>
          ) : filteredBooks.length === 0 ? (
            <div className="dashboard-empty">
              {bookSearch
                ? "No books match your search."
                : "No books are currently available."}
            </div>
          ) : (
            <div
              style={{
                display: "grid",
                gridTemplateColumns:
                  "repeat(auto-fit, minmax(240px, 1fr))",
                gap: "16px",
              }}
            >
              {filteredBooks.map((book) => (
                <div
                  key={book.bookId}
                  style={{
                    border: "1px solid #e2e6ec",
                    borderRadius: "10px",
                    padding: "18px",
                    background: "#fff",
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      gap: "12px",
                      alignItems: "flex-start",
                    }}
                  >
                    <div className="loan-book-icon">
                      <BookOpen size={18} />
                    </div>

                    <span
                      style={{
                        fontSize: "11px",
                        fontWeight: 600,
                        padding: "5px 8px",
                        borderRadius: "999px",
                        background:
                          Number(book.availableCopies) > 0
                            ? "#eef8f1"
                            : "#f5f5f5",
                        color:
                          Number(book.availableCopies) > 0
                            ? "#216e39"
                            : "#666",
                      }}
                    >
                      {Number(book.availableCopies) > 0
                        ? `${book.availableCopies} available`
                        : "Currently unavailable"}
                    </span>
                  </div>

                  <h3 style={{ margin: "14px 0 6px", fontSize: "16px" }}>
                    {book.title}
                  </h3>

                  <p
                    style={{
                      margin: "0 0 6px",
                      fontSize: "13px",
                      fontWeight: 600,
                    }}
                  >
                    {book.author}
                  </p>

                  <p
                    style={{
                      margin: "0 0 12px",
                      fontSize: "12px",
                      color: "#666",
                    }}
                  >
                    {book.category}
                  </p>

                  <p
                    style={{
                      margin: "0 0 14px",
                      fontSize: "12px",
                      lineHeight: 1.5,
                      color: "#555",
                    }}
                  >
                    {book.description || "No description available."}
                  </p>

                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      paddingTop: "10px",
                      borderTop: "1px solid #edf0f3",
                      fontSize: "11px",
                      color: "#666",
                    }}
                  >
                    <span>Fine: ₹{book.finePerDay}/day</span>
                    <span>Period: {book.borrowingPeriodDays} days</span>
                  </div>

                  {(() => {
                    const requestStatus = getBookRequestStatus(
                      book.bookId
                    );

                    const isPending = requestStatus === "PENDING";
                    const isBorrowed = requestStatus === "BORROWED";
                    const isUnavailable =
                      Number(book.availableCopies) <= 0;

                    return (
                      <button
                        type="button"
                        onClick={() => handleRequestBook(book)}
                        disabled={
                          isPending ||
                          isBorrowed ||
                          isUnavailable ||
                          requestingBookId === book.bookId
                        }
                        style={{
                          width: "100%",
                          marginTop: "14px",
                          padding: "9px 12px",
                          border: "none",
                          borderRadius: "7px",
                          cursor:
                            !isPending &&
                            !isBorrowed &&
                            !isUnavailable &&
                            requestingBookId !== book.bookId
                              ? "pointer"
                              : "not-allowed",
                          background:
                            isPending || isBorrowed || isUnavailable
                              ? "#e5e7eb"
                              : "#111827",
                          color:
                            isPending || isBorrowed || isUnavailable
                              ? "#777"
                              : "#fff",
                          fontSize: "12px",
                          fontWeight: 600,
                        }}
                      >
                        {requestingBookId === book.bookId
                          ? "Requesting..."
                          : isPending
                            ? "Request Pending"
                            : isBorrowed
                              ? "Currently Borrowed"
                              : isUnavailable
                                ? "Currently Unavailable"
                                : "Request to Borrow"}
                      </button>
                    );
                  })()}
                </div>
              ))}
            </div>
          )}
        </section>

<section className="dashboard-stats">



          <div className="stat-card">

            <div className="stat-icon">

              <BookOpen size={16} />

            </div>



            <div>

              <span>Active Loans</span>

              <strong>{activeLoans.length}</strong>

            </div>

          </div>



          <div className="stat-card">

            <div className="stat-icon">

              <Clock3 size={16} />

            </div>



            <div>

              <span>Due Soon</span>

              <strong>{dueSoon.length}</strong>

            </div>

          </div>



          <div className="stat-card">

            <div className="stat-icon">

              <AlertTriangle size={16} />

            </div>



            <div>

              <span>Overdue</span>

              <strong>{overdueLoans.length}</strong>

            </div>

          </div>



          <div className="stat-card">

            <div className="stat-icon">

              <CircleDollarSign size={16} />

            </div>



            <div>

              <span>Outstanding Fines</span>

              <strong>₹{outstandingFines}</strong>

            </div>

          </div>



          <div className="stat-card">

            <div className="stat-icon">

              <CheckCircle2 size={16} />

            </div>



            <div>

              <span>Returned Books</span>

              <strong>{returnedBooks.length}</strong>

            </div>

          </div>



        </section>



        {/* ==========================================

            OUTSTANDING FINES

        ========================================== */}



        {unpaidFines.length > 0 && (

          <section className="dashboard-card fine-card">



            <div className="card-header">



              <div>

                <p className="section-eyebrow">

                  PAYMENT REQUIRED

                </p>



                <h2>Outstanding Fines</h2>

              </div>



              <div className="fine-total">

                ₹{outstandingFines}

              </div>



            </div>



            <div className="fine-list">



              {unpaidFines.map((fine) => {



                const paymentPending =

                  fine.paymentStatus === "PENDING_APPROVAL";



                const paymentRejected =

                  fine.paymentStatus === "REJECTED";



                return (

                  <div

                    className="fine-item"

                    key={fine.fineId}

                  >



                    <div className="fine-item-icon">

                      <CircleDollarSign size={18} />

                    </div>



                    <div className="fine-details">



                      <h4>

                        Fine #{fine.fineId}

                      </h4>



                      <span>

                        Book #{fine.bookId} · Borrow #{fine.borrowId}

                      </span>



                      <span>

                        {fine.lateDays} late day

                        {fine.lateDays !== 1 ? "s" : ""} ·

                        ₹{fine.finePerDay}/day

                      </span>



                    </div>



                    <div className="fine-amount">

                      ₹{fine.fineAmount}

                    </div>



                    <div className="fine-payment-area">



                      {paymentPending ? (

                        <span className="payment-pending">

                          PAYMENT PENDING

                        </span>

                      ) : paymentRejected ? (

                        <button

                          className="pay-fine-button"

                          onClick={() =>

                            handlePayFine(fine)

                          }

                        >

                          <CreditCard size={13} />

                          Pay Again

                        </button>

                      ) : (

                        <button

                          className="pay-fine-button"

                          onClick={() =>

                            handlePayFine(fine)

                          }

                        >

                          <CreditCard size={13} />

                          Pay Fine

                        </button>

                      )}



                    </div>



                  </div>

                );

              })}



            </div>



          </section>

        )}



        {/* ==========================================

            CURRENT LOANS

        ========================================== */}



        <section className="dashboard-card">



          <div className="card-header">



            <div>

              <p className="section-eyebrow">

                LIBRARY ACTIVITY

              </p>



              <h2>Your current loans</h2>

            </div>



            <button

              className="download-button"

              onClick={downloadPDF}

              disabled={activeLoans.length === 0}

            >

              <Download size={13} />

              Download PDF

            </button>



          </div>



          {loading ? (

            <div className="dashboard-empty">

              Loading your loans...

            </div>

          ) : activeLoans.length === 0 ? (

            <div className="dashboard-empty">

              You currently have no active loans.

            </div>

          ) : (



            <div className="loan-list">



              {activeLoans.map((borrow) => {



                const daysRemaining =

                  getDaysDifference(borrow.dueDate);



                const isOverdue =

                  daysRemaining !== null &&

                  daysRemaining < 0;



                return (

                  <div

                    className={`loan-item ${

                      isOverdue

                        ? "loan-item-overdue"

                        : ""

                    }`}

                    key={borrow.borrowId}

                  >



                    <div className="loan-book-icon">

                      <BookOpen size={18} />

                    </div>



                    <div className="loan-details">



                      <h4>

                        Book #{borrow.bookId}

                      </h4>



                      <span>

                        Borrow ID: {borrow.borrowId}

                      </span>



                    </div>



                    <div className="loan-dates">



                      <span>Issue date</span>



                      <strong>

                        {borrow.issueDate || "-"}

                      </strong>



                    </div>



                    <div className="loan-dates">



                      <span>Due date</span>



                      <strong

                        className={

                          isOverdue

                            ? "overdue-date"

                            : ""

                        }

                      >

                        {borrow.dueDate || "-"}

                      </strong>



                    </div>



                    <span

                      className={`loan-status ${

                        isOverdue

                          ? "loan-status-overdue"

                          : ""

                      }`}

                    >

                      {isOverdue

                        ? "OVERDUE"

                        : "BORROWED"}

                    </span>



                    <button

                      className="return-button"

                      onClick={() =>

                        handleReturn(

                          borrow.borrowId

                        )

                      }

                      disabled={

                        returningId ===

                        borrow.borrowId

                      }

                    >

                      <RotateCcw size={13} />



                      {returningId ===

                      borrow.borrowId

                        ? "Returning..."

                        : "Return"}

                    </button>



                  </div>

                );

              })}



            </div>



          )}



        </section>



      </main>



      {/* ==========================================

          PAYMENT MODAL

      ========================================== */}



      {selectedFine && (

        <div

          className="payment-modal-overlay"

          onClick={() => {

            if (!submittingPayment) {

              setSelectedFine(null);

            }

          }}

        >



          <div

            className="payment-modal"

            onClick={(event) =>

              event.stopPropagation()

            }

          >



            <div className="payment-modal-header">



              <div>

                <p className="section-eyebrow">

                  FINE PAYMENT

                </p>



                <h2>Pay Fine</h2>

              </div>



              <button

                className="payment-close-button"

                onClick={() =>

                  setSelectedFine(null)

                }

                disabled={submittingPayment}

              >

                <X size={18} />

              </button>



            </div>



            <div className="payment-summary">



              <div>

                <span>Fine</span>

                <strong>

                  #{selectedFine.fineId}

                </strong>

              </div>



              <div>

                <span>Book</span>

                <strong>

                  #{selectedFine.bookId}

                </strong>

              </div>



              <div>

                <span>Late days</span>

                <strong>

                  {selectedFine.lateDays}

                </strong>

              </div>



              <div className="payment-summary-total">

                <span>Amount payable</span>

                <strong>

                  ₹{selectedFine.fineAmount}

                </strong>

              </div>



            </div>



            <div className="payment-instruction">



              <h3>

                Scan the administrator's PhonePe QR

              </h3>



              <p>

                Pay the exact fine amount using

                PhonePe or another supported UPI

                application.

              </p>



            </div>



            {/* ==========================================

                QR PLACEHOLDER

                Replace with actual admin QR later

            ========================================== */}



           <div className="qr-payment-box">

  <div className="real-qr-wrapper">

    <img

      src="/phonepe-qr.jpeg"

      alt="Admin PhonePe payment QR"

      className="real-phonepe-qr"

    />

  </div>

</div>



            <div className="payment-warning">

              After completing the UPI payment,

              click <strong>I Have Paid</strong>.

              Your payment will remain pending until

              an administrator verifies it.

            </div>



            <button

              className="confirm-payment-button"

              onClick={handlePaymentSubmitted}

              disabled={submittingPayment}

            >

              {submittingPayment

                ? "Submitting..."

                : "I Have Paid"}

            </button>



          </div>



        </div>

      )}



    </div>

  );

}



export default UserDashboard;