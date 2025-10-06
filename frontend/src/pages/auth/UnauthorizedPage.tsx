import { ShieldAlert } from 'lucide-react';
import { Link } from 'react-router-dom';

export const UnauthorizedPage = () => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <div className="max-w-md w-full bg-card border border-border rounded-2xl p-8 text-center shadow-lg">
        <div className="flex justify-center mb-6">
          <div className="h-20 w-20 bg-red-100 dark:bg-red-900/30 rounded-full flex items-center justify-center">
            <ShieldAlert className="h-10 w-10 text-red-600 dark:text-red-500" />
          </div>
        </div>
        <h1 className="text-2xl font-bold text-foreground mb-2">Access Denied</h1>
        <p className="text-foreground/70 mb-8">
          You do not have the necessary permissions to view this page or perform this action.
        </p>
        <Link 
          to="/"
          className="inline-flex items-center justify-center bg-primary-600 hover:bg-primary-700 text-white px-6 py-3 rounded-lg font-medium transition-colors"
        >
          Return to Dashboard
        </Link>
      </div>
    </div>
  );
};
