import 'package:go_router/go_router.dart';

// Import views
import '../../views/auth/login_screen.dart';
import '../../views/auth/register_screen.dart';
import '../../views/auth/forgot_password_screen.dart';
import '../../views/auth/reset_password_screen.dart';

import '../../views/dashboard/dashboard_screen.dart';
import '../../views/profile/view_profile_screen.dart';
import '../../views/profile/edit_profile_screen.dart';
import '../../views/profile/my_addresses_screen.dart';

import '../../views/guides/list_guides_screen.dart';
import '../../views/guides/create_guide_screen.dart';
import '../../views/guides/edit_guide_screen.dart';
import '../../views/tracking/tracking_screen.dart';
import '../../views/payment/payment_screen.dart';

import '../../views/delivery/delivery_list_screen.dart';
import '../../views/delivery/delivery_map_screen.dart';

import '../../views/admin/admin_users_screen.dart';
import '../../views/admin/admin_edit_user_screen.dart';
import '../../views/admin/admin_payments_screen.dart';
import '../../views/admin/admin_warroom_screen.dart';

import '../../views/static/landing_screen.dart';
import '../../views/static/about_us_screen.dart';
import '../../views/static/support_screen.dart';

final appRouter = GoRouter(
  initialLocation: '/',
  routes: [
    // Landing Route
    GoRoute(
      path: '/',
      builder: (context, state) => const LandingScreen(),
    ),
    
    // Auth Routes
    GoRoute(
      path: '/login',
      builder: (context, state) => const LoginScreen(),
    ),
    GoRoute(
      path: '/register',
      builder: (context, state) => const RegisterScreen(),
    ),
    GoRoute(
      path: '/forgot-password',
      builder: (context, state) => const ForgotPasswordScreen(),
    ),
    GoRoute(
      path: '/reset-password',
      builder: (context, state) => const ResetPasswordScreen(),
    ),

    // Dashboard
    GoRoute(
      path: '/dashboard',
      builder: (context, state) => const DashboardScreen(),
    ),

    // Profile Routes
    GoRoute(
      path: '/profile',
      builder: (context, state) => const ViewProfileScreen(),
    ),
    GoRoute(
      path: '/profile/edit',
      builder: (context, state) => const EditProfileScreen(),
    ),
    GoRoute(
      path: '/profile/addresses',
      builder: (context, state) => const MyAddressesScreen(),
    ),

    // Guides & Shipping
    GoRoute(
      path: '/guides',
      builder: (context, state) => const ListGuidesScreen(),
    ),
    GoRoute(
      path: '/guides/create',
      builder: (context, state) => const CreateGuideScreen(),
    ),
    GoRoute(
      path: '/guides/edit/:id',
      builder: (context, state) => const EditGuideScreen(),
    ),
    GoRoute(
      path: '/tracking',
      builder: (context, state) => const TrackingScreen(),
    ),
    GoRoute(
      path: '/payment',
      builder: (context, state) => const PaymentScreen(),
    ),

    // Delivery (Repartidor)
    GoRoute(
      path: '/delivery',
      builder: (context, state) => const DeliveryListScreen(),
    ),
    GoRoute(
      path: '/delivery/map',
      builder: (context, state) => const DeliveryMapScreen(),
    ),

    // Admin Routes
    GoRoute(
      path: '/admin/users',
      builder: (context, state) => const AdminUsersScreen(),
    ),
    GoRoute(
      path: '/admin/users/edit/:id',
      builder: (context, state) => const AdminEditUserScreen(),
    ),
    GoRoute(
      path: '/admin/payments',
      builder: (context, state) => const AdminPaymentsScreen(),
    ),
    GoRoute(
      path: '/admin/warroom',
      builder: (context, state) => const AdminWarroomScreen(),
    ),

    // Static
    GoRoute(
      path: '/about-us',
      builder: (context, state) => const AboutUsScreen(),
    ),
    GoRoute(
      path: '/support',
      builder: (context, state) => const SupportScreen(),
    ),
  ],
);
