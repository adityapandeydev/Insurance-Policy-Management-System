import { ReactNode } from 'react';

type BadgeVariant = 'success' | 'warning' | 'danger' | 'info' | 'default';

interface BadgeProps {
  children: ReactNode;
  variant?: BadgeVariant;
  className?: string;
}

const variantStyles: Record<BadgeVariant, string> = {
  success: 'bg-green-100 text-green-600 font-bold dark:bg-green-500/30 dark:text-green-400 border border-green-300 dark:border-green-800/50 shadow-sm',
  warning: 'bg-orange-100 text-orange-800 font-bold dark:bg-orange-900/30 dark:text-orange-400 border border-orange-300 dark:border-orange-800/50 shadow-sm',
  danger: 'bg-red-100 text-red-800 font-bold dark:bg-red-900/30 dark:text-red-400 border border-red-300 dark:border-red-800/50 shadow-sm',
  info: 'bg-blue-100 text-blue-800 font-bold dark:bg-blue-900/30 dark:text-blue-400 border border-blue-300 dark:border-blue-800/50 shadow-sm',
  default: 'bg-gray-100 text-gray-800 font-bold dark:bg-gray-800 dark:text-gray-400 border border-gray-300 dark:border-gray-700 shadow-sm',
};

export const Badge = ({ children, variant = 'default', className = '' }: BadgeProps) => {
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${variantStyles[variant]} ${className}`}>
      {children}
    </span>
  );
};
